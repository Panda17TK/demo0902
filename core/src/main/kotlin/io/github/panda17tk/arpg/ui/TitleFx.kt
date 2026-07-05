package io.github.panda17tk.arpg.ui

import io.github.panda17tk.arpg.math.Rng
import kotlin.math.sin

/**
 * v2.71 タイトル演出 — the sky's little dramas, all pure functions of time so the front door
 * stays deterministic and testable: an occasional meteor, per-star twinkle, drifting nebulae.
 * Coordinates are screen fractions (0..1, y-up); the screen scales them.
 */
object TitleFx {
    /** One falling light: position (fractions), direction of travel, and progress 0..1. */
    data class Meteor(val fx: Float, val fy: Float, val dirX: Float, val dirY: Float, val p: Float)

    const val CYCLE = 9f    // seconds per meteor window
    const val DURATION = 1.4f // seconds a meteor takes to cross

    /** The meteor visible at [t] seconds, or null between falls. Deterministic per cycle. */
    fun meteorAt(t: Float): Meteor? {
        if (t < 0f) return null
        val cycle = (t / CYCLE).toInt()
        val r = Rng(cycle * 977L + 31L)
        val start = r.nextFloat() * (CYCLE - DURATION - 1f) // a quiet tail keeps falls sparse
        val local = t - cycle * CYCLE - start
        if (local < 0f || local > DURATION) return null
        val p = local / DURATION
        val fx0 = 0.25f + r.nextFloat() * 0.6f  // starts high, upper-middle band
        val fy0 = 0.80f + r.nextFloat() * 0.15f
        val dx = -(0.22f + r.nextFloat() * 0.14f) // falls down-left, like everything adrift here
        val dy = -(0.16f + r.nextFloat() * 0.10f)
        return Meteor(fx0 + dx * p, fy0 + dy * p, dx, dy, p)
    }

    /** Per-star brightness at [t] — each index breathes on its own rate and phase, 0.35..1. */
    fun twinkle(i: Int, t: Float): Float =
        0.675f + 0.325f * sin(t * (0.8f + (i % 7) * 0.23f) + i * 1.7f)

    /** The logo halo's breathing alpha at [t] — slow, quiet, never fully dark. */
    fun glow(t: Float): Float = 0.10f + 0.06f * sin(t * 0.6f)

    /** Nebula clouds: fraction anchors + drift speed + radius (fraction of min dimension). */
    data class Cloud(val fx: Float, val fy: Float, val speed: Float, val fr: Float)

    val CLOUDS: List<Cloud> = run {
        val r = Rng(11L)
        List(4) {
            Cloud(
                fx = r.nextFloat(),
                fy = 0.25f + r.nextFloat() * 0.6f,
                speed = 1.5f + r.nextFloat() * 2.5f, // px/second at screen scale — barely moving
                fr = 0.18f + r.nextFloat() * 0.14f,
            )
        }
    }
}
