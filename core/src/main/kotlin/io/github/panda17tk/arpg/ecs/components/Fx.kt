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

    class Beam(val sx: Float, val sy: Float, val ex: Float, val ey: Float, var t: Float, val life: Float)
    class Slash(val x: Float, val y: Float, val ang: Float, var t: Float, val life: Float)
    class After(val x: Float, val y: Float, val w: Float, val h: Float, val color: Color, var t: Float, val life: Float)

    var shakeT = 0f
        private set
    var shakeMag = 0f
        private set

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

    fun spawnBeam(sx: Float, sy: Float, ex: Float, ey: Float) { beams.add(Beam(sx, sy, ex, ey, 0f, 0.12f)) }
    fun spawnSlash(x: Float, y: Float, ang: Float) { slashes.add(Slash(x, y, ang, 0f, 0.22f)) }
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

    /** Advance particles + decay shake. Cosmetic, called once per render frame. */
    fun update(dt: Float) {
        if (shakeT > 0f) {
            shakeT -= dt
            if (shakeT <= 0f) { shakeT = 0f; shakeMag = 0f }
        }
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
    }
}
