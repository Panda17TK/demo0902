package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import kotlin.math.cos
import kotlin.math.sin

/** One drifting bit of the void: a small piece of debris or a larger asteroid, tumbling across space. */
class Drifter(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val asteroid: Boolean,
    var rot: Float,
    val rotSpeed: Float,
)

/** The cosmetic field of drifting debris/asteroids that flows around the player in space. */
class DriftField(val items: List<Drifter>)

/**
 * Pure motion for the drifting space debris/asteroids. Cosmetic — seeded by WorldFactory, advanced and drawn
 * by GameScreen, never touched by the sim. Pieces wrap toroidally around the player so the void always feels
 * populated however far you fly through the (now much larger) space. No libGDX/Fleks → unit-testable.
 */
object Drift {
    private const val TAU = 6.2831855f

    /** Scatter [count] drifters across a 2·[range] box centred on (cx,cy), each with a slow heading + spin. */
    fun field(rng: Rng, count: Int, cx: Float, cy: Float, range: Float): DriftField {
        val out = ArrayList<Drifter>(count)
        repeat(count) {
            val a = rng.nextFloat() * TAU
            val sp = MIN_SPEED + rng.nextFloat() * (MAX_SPEED - MIN_SPEED)
            val asteroid = rng.nextFloat() < ASTEROID_FRAC
            val size = if (asteroid) AST_MIN + rng.nextFloat() * (AST_MAX - AST_MIN) else DEB_MIN + rng.nextFloat() * (DEB_MAX - DEB_MIN)
            out.add(
                Drifter(
                    x = cx + (rng.nextFloat() * 2f - 1f) * range,
                    y = cy + (rng.nextFloat() * 2f - 1f) * range,
                    vx = cos(a) * sp, vy = sin(a) * sp, size = size, asteroid = asteroid,
                    rot = rng.nextFloat() * TAU, rotSpeed = (rng.nextFloat() * 2f - 1f) * MAX_SPIN,
                ),
            )
        }
        return DriftField(out)
    }

    /** Drift + spin each piece, wrapping it toroidally into the 2·[range] box centred on the player. */
    fun advance(field: DriftField, px: Float, py: Float, range: Float, dt: Float) {
        val span = range * 2f
        for (d in field.items) {
            d.x += d.vx * dt; d.y += d.vy * dt; d.rot += d.rotSpeed * dt
            if (d.x - px > range) d.x -= span else if (px - d.x > range) d.x += span
            if (d.y - py > range) d.y -= span else if (py - d.y > range) d.y += span
        }
    }

    private const val MIN_SPEED = 8f       // slow ambient drift…
    private const val MAX_SPEED = 34f      // …up to a brisker tumble
    private const val ASTEROID_FRAC = 0.22f // ~1 in 5 pieces is a bigger asteroid
    private const val DEB_MIN = 2.5f
    private const val DEB_MAX = 6f
    private const val AST_MIN = 12f
    private const val AST_MAX = 34f
    private const val MAX_SPIN = 1.4f
}
