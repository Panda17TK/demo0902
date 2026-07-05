package io.github.panda17tk.arpg.ecs.components

import com.badlogic.gdx.graphics.Color
import io.github.panda17tk.arpg.math.Rng
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Cosmetic effects buffer (legacy state.fx + state.shake): death gibs and screen shake.
 * Systems only *push* effects; GameScreen advances + renders them. Never touches the sim, and
 * uses its OWN [rng] so visual randomness can't desync the deterministic gameplay RNG.
 */
class Fx(private val rng: Rng = Rng(0x5DEECE66DL)) {
    class Particle(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var t: Float, val life: Float, val size: Float, val color: Color, val gravity: Boolean,
    )

    val particles = ArrayList<Particle>(256)
    val beams = ArrayList<Beam>(8)
    val slashes = ArrayList<Slash>(8)
    val afters = ArrayList<After>(64)
    val pops = ArrayList<Pop>(32)       // v2.85: floating damage numbers
    val corpses = ArrayList<Corpse>(16) // v2.85: bodies squashing out before the gibs
    val bursts = ArrayList<Burst>(8)    // v2.85: delayed explosion stages (big deaths chain)
    val warnRings = ArrayList<Ring>(4)  // v2.86: the spawn-point warning of a heavy arrival

    class Beam(val sx: Float, val sy: Float, val ex: Float, val ey: Float, var t: Float, val life: Float, val width: Float = 1.8f)
    class Slash(val x: Float, val y: Float, val ang: Float, var t: Float, val life: Float, val scale: Float = 1f)
    class After(val x: Float, val y: Float, val w: Float, val h: Float, val color: Color, var t: Float, val life: Float)
    class Pop(var x: Float, var y: Float, val text: String, val color: Color, val scale: Float, var t: Float = 0f, val life: Float = 0.7f)
    class Corpse(val x: Float, val y: Float, val w: Float, val h: Float, val color: Color, val big: Boolean, var t: Float = 0f, val life: Float)
    class Burst(val x: Float, val y: Float, val color: Color, val big: Boolean, var delay: Float)
    class Ring(val x: Float, val y: Float, var t: Float = 0f, val life: Float = 1.1f, val maxR: Float = 72f)

    var shakeT = 0f
        private set
    var shakeMag = 0f
        private set

    // ── v2.85 game feel: hitstop / slow-mo / camera kick ──────────────────
    // Cosmetic time control lives HERE (not in the sim): systems request, the screen
    // reads simTimeScale() when stepping. The sim just runs fewer steps — determinism holds.
    var hitstopT = 0f
        private set
    var slowmoT = 0f
        private set
    var kickX = 0f
        private set
    var kickY = 0f
        private set

    /** Freeze the sim for [d] seconds (max-combined) — the frame-hold that sells an impact. */
    fun hitstop(d: Float) { if (d > hitstopT) hitstopT = d }

    /** Run the sim at quarter speed for [d] seconds (max-combined) — the big-kill exhale. */
    fun slowmo(d: Float) { if (d > slowmoT) slowmoT = d }

    /** What the screen multiplies the sim delta by this frame. Hitstop outranks slow-mo. */
    fun simTimeScale(): Float = when {
        hitstopT > 0f -> 0f
        slowmoT > 0f -> SLOWMO_SCALE
        else -> 1f
    }

    /** A directional camera punch (recoil); decays exponentially in [update]. */
    fun addKick(dx: Float, dy: Float) { kickX += dx; kickY += dy }

    /** Combine shake additively-by-max so the strongest recent hit wins (legacy addShake). */
    fun addShake(t: Float, mag: Float) {
        if (t > shakeT) shakeT = t
        if (mag > shakeMag) shakeMag = mag
    }

    fun shakeX(): Float = if (shakeMag > 0f) (rng.nextFloat() * 2f - 1f) * shakeMag else 0f
    fun shakeY(): Float = if (shakeMag > 0f) (rng.nextFloat() * 2f - 1f) * shakeMag else 0f

    /** Death burst: gibs flung outward + a center flash (legacy spawnDeathFX). */
    fun spawnDeath(x: Float, y: Float, color: Color, big: Boolean) {
        val n = if (big) 22 else 12
        repeat(n) {
            val a = rng.nextFloat() * TWO_PI
            val sp = (if (big) 120f else 80f) + rng.nextFloat() * (if (big) 200f else 140f)
            add(x, y, cos(a) * sp, sin(a) * sp - 40f, 0.45f + rng.nextFloat() * 0.35f, 2f + rng.nextFloat() * (if (big) 4f else 3f), color, gravity = true)
        }
        add(x, y, 0f, 0f, 0.22f, if (big) 40f else 24f, FLASH, gravity = false)
    }

    fun spawnSparks(x: Float, y: Float, n: Int, color: Color) {
        repeat(n) {
            val a = rng.nextFloat() * TWO_PI
            val sp = 60f + rng.nextFloat() * 120f
            add(x, y, cos(a) * sp, sin(a) * sp, 0.25f, 2f, color, gravity = false)
        }
    }

    /** v2.85 段階的な死: the body squashes out first, THEN bursts — big ones chain three blasts. */
    fun spawnDeathStaged(x: Float, y: Float, w: Float, h: Float, color: Color, big: Boolean) {
        corpses.add(Corpse(x, y, w, h, color, big, life = if (big) 0.34f else 0.24f))
        if (big) {
            for (i in 0 until 3) {
                val ox = (rng.nextFloat() * 2f - 1f) * w * 0.4f
                val oy = (rng.nextFloat() * 2f - 1f) * h * 0.4f
                bursts.add(Burst(x + ox, y + oy, color, big = i == 2, delay = i * 0.12f))
            }
        } else {
            bursts.add(Burst(x, y, color, big = false, delay = 0.10f))
        }
    }

    /** v2.85 damage pop: a small number that floats up and fades ([amount] rounded up). */
    fun spawnPop(x: Float, y: Float, amount: Int, color: Color, scale: Float = 1f) {
        if (amount <= 0) return
        pops.add(Pop(x, y, amount.toString(), color, scale))
    }

    /** v2.86: a heavy arrival announces its spot — an expanding warning ring at the spawn point. */
    fun spawnWarnRing(x: Float, y: Float) { warnRings.add(Ring(x, y)) }

    fun spawnBeam(sx: Float, sy: Float, ex: Float, ey: Float, width: Float = 1.8f) { beams.add(Beam(sx, sy, ex, ey, 0f, 0.12f, width)) }
    fun spawnSlash(x: Float, y: Float, ang: Float, scale: Float = 1f) { slashes.add(Slash(x, y, ang, 0f, 0.22f, scale)) }
    fun spawnAfterimage(x: Float, y: Float, w: Float, h: Float, color: Color) { afters.add(After(x, y, w, h, color, 0f, 0.25f)) }
    /** Rock chips flung from a struck wall (gravity gibs). */
    fun spawnChips(x: Float, y: Float, n: Int, color: Color) {
        repeat(n) {
            val a = rng.nextFloat() * TWO_PI
            val sp = 50f + rng.nextFloat() * 90f
            add(x, y, cos(a) * sp, sin(a) * sp - 30f, 0.3f + rng.nextFloat() * 0.22f, 1.4f + rng.nextFloat() * 1.6f, color, gravity = true)
        }
    }

    private fun add(x: Float, y: Float, vx: Float, vy: Float, life: Float, size: Float, color: Color, gravity: Boolean) {
        particles.add(Particle(x, y, vx, vy, 0f, life, size, color, gravity))
    }

    /** Advance particles + decay shake. Cosmetic, called once per render frame with REAL dt. */
    fun update(dt: Float) {
        if (shakeT > 0f) {
            shakeT -= dt
            if (shakeT <= 0f) { shakeT = 0f; shakeMag = 0f }
        }
        // v2.85: hitstop runs down first, then slow-mo; the camera kick decays exponentially.
        if (hitstopT > 0f) hitstopT = (hitstopT - dt).coerceAtLeast(0f)
        else if (slowmoT > 0f) slowmoT = (slowmoT - dt).coerceAtLeast(0f)
        val kickDecay = 0.000006f.pow(dt) // ~gone in a tenth of a second
        kickX *= kickDecay; kickY *= kickDecay
        for (i in pops.indices.reversed()) {
            val p = pops[i]
            p.t += dt; p.y -= POP_RISE * dt
            if (p.t >= p.life) pops.removeAt(i)
        }
        for (i in corpses.indices.reversed()) { val c = corpses[i]; c.t += dt; if (c.t >= c.life) corpses.removeAt(i) }
        for (i in bursts.indices.reversed()) {
            val b = bursts[i]
            b.delay -= dt
            if (b.delay <= 0f) { spawnDeath(b.x, b.y, b.color, b.big); bursts.removeAt(i) }
        }
        for (i in warnRings.indices.reversed()) { val r = warnRings[i]; r.t += dt; if (r.t >= r.life) warnRings.removeAt(i) }
        for (i in particles.indices.reversed()) {
            val p = particles[i]
            p.t += dt
            if (p.gravity) { p.vy += GRAVITY * dt; p.vx *= 0.12f.pow(dt) }
            p.x += p.vx * dt; p.y += p.vy * dt
            if (p.t >= p.life) particles.removeAt(i)
        }
        for (i in beams.indices.reversed()) { val b = beams[i]; b.t += dt; if (b.t >= b.life) beams.removeAt(i) }
        for (i in slashes.indices.reversed()) { val sl = slashes[i]; sl.t += dt; if (sl.t >= sl.life) slashes.removeAt(i) }
        for (i in afters.indices.reversed()) { val a = afters[i]; a.t += dt; if (a.t >= a.life) afters.removeAt(i) }
    }

    companion object {
        private val TWO_PI = (Math.PI * 2.0).toFloat()
        private const val GRAVITY = 320f
        private val FLASH = Color(1f, 0.95f, 0.8f, 1f)
        const val SLOWMO_SCALE = 0.3f // v2.85: the big-kill exhale runs at this speed
        private const val POP_RISE = 46f // v2.85: damage numbers float up this fast (world px/s)
    }
}
