package io.github.panda17tk.arpg.sim

/**
 * High-speed impact (crash) maths: how much damage a slam deals and how momentum rebounds.
 * Pure for unit testing; tuning lives in the systems that call it.
 */
object CrashModel {
    /** Damage from an impact: zero below [threshold], then proportional to the excess speed. */
    fun damage(inwardSpeed: Float, threshold: Float, k: Float): Float =
        if (inwardSpeed <= threshold) 0f else k * (inwardSpeed - threshold)

    /**
     * Reflect the inward part of (driftX,driftY) about the outward normal (nx,ny) with [restitution].
     * Outward/tangential motion is left unchanged; inward motion bounces back at restitution·speed.
     */
    fun rebound(driftX: Float, driftY: Float, nx: Float, ny: Float, restitution: Float): Pair<Float, Float> {
        val vn = driftX * nx + driftY * ny // component along the outward normal (negative = moving inward)
        if (vn >= 0f) return driftX to driftY
        val delta = -vn * (1f + restitution) // raise the normal component to +restitution·|vn|
        return (driftX + nx * delta) to (driftY + ny * delta)
    }
}
