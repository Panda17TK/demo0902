package io.github.panda17tk.arpg.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Procedural player + enemy drawing ported from legacy render/enemy-sprites.js + renderer.js.
 * All draws run inside a Filled ShapeRenderer pass (caller manages begin/end + blend). World is
 * y-down (matches the legacy canvas), so -y is "up" on screen.
 */
object Actors {
    private val TAU = (Math.PI * 2.0).toFloat()

    private val SHADOW = Color(0f, 0f, 0f, 0.30f)
    private val PLAYER = Color.valueOf("7ab0ff")
    private val PLAYER_HIT = Color.valueOf("ff9aa2")
    private val BELLY = Color(1f, 1f, 1f, 0.18f)
    private val GUN = Color.valueOf("2b3a50")
    private val GUN_TIP = Color.valueOf("1a2738")
    private val MUZZLE = Color.valueOf("fff1c0")
    private val THRUST_OUT = Color(1f, 0.40f, 0.10f, 0.45f)  // soft orange plume (the trailing tail)
    private val THRUST_MID = Color(1f, 0.62f, 0.18f, 0.80f)  // orange body
    private val THRUST_CORE = Color(1f, 0.93f, 0.70f, 0.95f) // hot near-white nozzle
    private val EYE_DARK = Color.valueOf("16202e")
    private val RED_EYE = Color.valueOf("ff5a5a")
    private val BOSS_EYE = Color.valueOf("ffd166")
    private val SPITTER_EYE = Color.valueOf("0a0f14")
    private val STALKER_EYE = Color.valueOf("d6c2ff")
    private val BAR_BG = Color(0f, 0f, 0f, 0.6f)
    private val BAR_HI = Color.valueOf("7fe08a")
    private val BAR_MID = Color.valueOf("e0d27f")
    private val BAR_LO = Color.valueOf("e08a7f")

    private val tmp = Color()      // for shade()
    private val baseTmp = Color()  // for the per-mob base color (flash/dodge)
    private val dashTmp = Color()

    private fun shade(c: Color, amt: Int): Color { val d = amt / 255f; return tmp.set(c).add(d, d, d, 0f).clamp() }
    private fun ellipseC(s: ShapeRenderer, cx: Float, cy: Float, rx: Float, ry: Float) = s.ellipse(cx - rx, cy - ry, rx * 2f, ry * 2f)

    private fun eyes(s: ShapeRenderer, cx: Float, cy: Float, spread: Float, ry: Float, fx: Float, fy: Float, pupil: Color, r: Float, white: Boolean) {
        val l = hypot(fx, fy).let { if (it == 0f) 1f else it }
        val ex = fx / l * 3f; val ey = fy / l * 2f
        if (white) { s.color = Color.WHITE; s.circle(cx - spread, cy + ry, r + 1f, 8); s.circle(cx + spread, cy + ry, r + 1f, 8) }
        s.color = pupil; s.circle(cx - spread + ex, cy + ry + ey, r, 8); s.circle(cx + spread + ex, cy + ry + ey, r, 8)
    }

    fun drawPlayer(s: ShapeRenderer, x: Float, y: Float, faceX: Float, faceY: Float, dashing: Boolean, hitFlash: Boolean, muzzle: Boolean, t: Float = 0f) {
        val r = Tuning.PLAYER_RADIUS; val w = r * 2f; val h = r * 2f
        s.color = SHADOW; ellipseC(s, x, y + h / 2f - 1f, w * 0.46f, h * 0.18f)
        if (dashing) {
            s.color = dashTmp.set(PLAYER).also { it.a = 0.25f }; Draw.roundedRect(s, x - faceX * 8f - w / 2f, y - faceY * 8f - h / 2f, w, h, 5f)
            // Two thruster flames out the back (opposite facing), one off each shoulder, with a slight flicker tail.
            val bx = -faceX; val by = -faceY; val perpX = -faceY; val perpY = faceX
            val flick = 0.82f + 0.18f * sin(t * 42f)
            for (sgn in intArrayOf(-1, 1)) {
                val nx = x + bx * (r * 0.55f) + perpX * sgn * r * 0.42f
                val ny = y + by * (r * 0.55f) + perpY * sgn * r * 0.42f
                s.color = THRUST_OUT; s.circle(nx + bx * 9f * flick, ny + by * 9f * flick, r * 0.30f * flick, 8)  // trailing tail
                s.color = THRUST_MID; s.circle(nx + bx * 5f * flick, ny + by * 5f * flick, r * 0.40f * flick, 8)  // body
                s.color = THRUST_CORE; s.circle(nx + bx * 1.5f, ny + by * 1.5f, r * 0.26f * flick, 8)             // hot nozzle
            }
        }
        s.color = GUN; Draw.orientedRect(s, x, y, faceX, faceY, 6f, 12f, 2.5f)
        s.color = GUN_TIP; Draw.orientedRect(s, x, y, faceX, faceY, 15f, 3f, 1.5f)
        if (muzzle) { s.color = MUZZLE; Draw.orientedRect(s, x, y, faceX, faceY, 18f, 6f, 3.5f) }
        s.color = if (hitFlash) PLAYER_HIT else PLAYER
        Draw.roundedRect(s, x - w / 2f, y - h / 2f, w, h, 6f)
        s.color = BELLY; Draw.roundedRect(s, x - w / 2f + 3f, y - h / 2f + 3f, w - 6f, h * 0.45f, 4f)
        eyes(s, x, y, 4f, -2f, faceX, faceY, EYE_DARK, 1.5f, true)
    }

    @Suppress("LongParameterList")
    fun drawMob(
        s: ShapeRenderer, kind: String, tier: String, x: Float, y: Float, w: Float, h: Float, color: Color,
        faceX: Float, faceY: Float, moving: Boolean, hitFlash: Boolean, dodge: Boolean,
        chargeProg: Float, enrage: Boolean, hpFrac: Float, animSeed: Float, time: Float,
    ) {
        val elite = tier == "midboss" || tier == "boss"
        s.color = SHADOW; ellipseC(s, x, y + h / 2f - 1f, w * 0.46f, h * 0.18f)
        val base = when {
            dodge -> baseTmp.set(Color.WHITE).also { it.a = 0.55f }
            hitFlash -> baseTmp.set(Color.WHITE)
            else -> baseTmp.set(color)
        }
        when {
            elite -> elite(s, x, y, w, h, base, color, faceX, faceY, enrage, tier == "boss", time, animSeed)
            kind == "zombie" -> zombie(s, x, y, w, h, base, color, faceX, faceY, moving, chargeProg, time, animSeed)
            kind == "spitter" -> spitter(s, x, y, w, h, base, color, faceX, faceY, time, animSeed)
            kind == "stalker" -> stalker(s, x, y, w, h, base, color, faceX, faceY)
            else -> { s.color = base; Draw.roundedRect(s, x - w / 2f, y - h / 2f, w, h, 4f); eyes(s, x, y, 4f, -3f, faceX, faceY, EYE_DARK, 2.2f, true) }
        }
        if (elite || hpFrac < 1f) {
            val bw = if (elite) maxOf(w, 40f) else w
            val by = y - h / 2f - (if (elite) 12f else 8f)
            val bh = if (elite) 4f else 3f
            s.color = BAR_BG; s.rect(x - bw / 2f, by, bw, bh)
            s.color = if (hpFrac > 0.5f) BAR_HI else if (hpFrac > 0.25f) BAR_MID else BAR_LO
            s.rect(x - bw / 2f, by, bw * hpFrac.coerceIn(0f, 1f), bh)
        }
    }

    private fun zombie(s: ShapeRenderer, x: Float, y: Float, w: Float, h: Float, base: Color, color: Color, fx: Float, fy: Float, moving: Boolean, chargeProg: Float, time: Float, seed: Float) {
        val by = sin(time * 9f + seed) * (if (moving) 1.6f else 0.6f)
        val cy = y + by
        s.color = base; Draw.roundedRect(s, x - w / 2f, cy - h / 2f, w, h, 5f)
        s.color = shade(color, 18); Draw.roundedRect(s, x - w / 2f + 3f, cy - h / 2f + 3f, w - 6f, h * 0.5f, 4f)
        s.color = shade(color, -22)
        if (chargeProg >= 0f) {
            val raise = -6f - chargeProg * 6f
            s.rect(x - w / 2f - 2f, cy + raise, 4f, 10f); s.rect(x + w / 2f - 2f, cy + raise, 4f, 10f)
        } else {
            val sway = sin(time * 9f + seed) * 2f
            s.rect(x - w / 2f - 2f, cy - 2f + sway, 4f, 9f); s.rect(x + w / 2f - 2f, cy - 2f - sway, 4f, 9f)
        }
        eyes(s, x, cy, 4f, -2f, fx, fy, RED_EYE, 2.2f, true)
    }

    private fun spitter(s: ShapeRenderer, x: Float, y: Float, w: Float, h: Float, base: Color, color: Color, fx: Float, fy: Float, time: Float, seed: Float) {
        s.color = base; ellipseC(s, x, y, w / 2f, h / 2f)
        s.color = shade(color, 30)
        for (i in 0 until 4) { val a = i / 4f * TAU + time + seed; s.circle(x + cos(a) * 4f, y + sin(a) * 3f, 1.4f, 6) }
        val l = hypot(fx, fy).let { if (it == 0f) 1f else it }
        s.color = shade(color, -34); s.circle(x + fx / l * 7.2f, y + fy / l * 4.8f + 1f, 3f, 8)
        eyes(s, x, y, 0f, -3f, fx, fy, SPITTER_EYE, 3f, true)
    }

    private fun stalker(s: ShapeRenderer, x: Float, y: Float, w: Float, h: Float, base: Color, color: Color, fx: Float, fy: Float) {
        val ang = atan2(fy, fx); val c = cos(ang); val sn = sin(ang)
        fun rx(lx: Float, ly: Float) = x + lx * c - ly * sn
        fun ry(lx: Float, ly: Float) = y + lx * sn + ly * c
        s.color = base
        s.triangle(rx(w * 0.6f, 0f), ry(w * 0.6f, 0f), rx(0f, h * 0.42f), ry(0f, h * 0.42f), rx(-w * 0.5f, 0f), ry(-w * 0.5f, 0f))
        s.triangle(rx(w * 0.6f, 0f), ry(w * 0.6f, 0f), rx(-w * 0.5f, 0f), ry(-w * 0.5f, 0f), rx(0f, -h * 0.42f), ry(0f, -h * 0.42f))
        s.color = STALKER_EYE
        s.circle(rx(3f, -2f), ry(3f, -2f), 1.3f, 6); s.circle(rx(3f, 2f), ry(3f, 2f), 1.3f, 6)
    }

    private fun elite(s: ShapeRenderer, x: Float, y: Float, w: Float, h: Float, base: Color, color: Color, fx: Float, fy: Float, enrage: Boolean, boss: Boolean, time: Float, seed: Float) {
        val cy = y + sin(time * 3f + seed) * 1.2f * 0.3f
        s.color = base; Draw.roundedRect(s, x - w / 2f, cy - h / 2f, w, h, 8f)
        s.color = shade(color, -26); Draw.roundedRect(s, x - w / 2f, cy - h / 2f, w, h * 0.32f, 8f)
        s.color = shade(color, 20); Draw.roundedRect(s, x - w / 2f + 4f, cy + h * 0.06f, w - 8f, h * 0.34f, 5f)
        s.color = shade(color, -40)
        for (sx in intArrayOf(-1, 1)) {
            val sf = sx.toFloat()
            s.triangle(x + sf * (w / 2f - 2f), cy - h / 2f + 4f, x + sf * (w / 2f + 6f), cy - h / 2f - 2f, x + sf * (w / 2f - 2f), cy - h / 2f + 12f)
        }
        val l = hypot(fx, fy).let { if (it == 0f) 1f else it }
        s.color = if (enrage) RED_EYE else BOSS_EYE
        for (ex in intArrayOf(-5, 5)) for (ey in intArrayOf(-3, 1)) s.circle(x + ex + fx / l * 1.8f, cy + ey + fy / l * 1.2f, 1.7f, 6)
        if (boss) {
            s.color = BOSS_EYE
            val ccy = cy - h / 2f - 4f
            s.triangle(x - 8f, ccy + 6f, x - 8f, ccy, x - 4f, ccy + 4f)
            s.triangle(x - 4f, ccy + 4f, x, ccy - 2f, x + 4f, ccy + 4f)
            s.triangle(x + 4f, ccy + 4f, x + 8f, ccy, x + 8f, ccy + 6f)
        }
    }
}
