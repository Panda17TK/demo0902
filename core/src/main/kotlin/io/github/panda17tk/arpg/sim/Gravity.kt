package io.github.panda17tk.arpg.sim

/**
 * How gravity acceleration feeds an entity's momentum (drift): scaled by the entity's response
 * (0 = ignores gravity, 1 = normal, >1 = heavy) and a dash-escape multiplier (the player weakens
 * gravity while dashing). Pure for unit testing.
 */
object Gravity {
    fun applyToDrift(
        driftX: Float, driftY: Float, ax: Float, ay: Float, response: Float, dashMul: Float, dt: Float,
    ): Pair<Float, Float> {
        val k = response * dashMul * dt
        return (driftX + ax * k) to (driftY + ay * k)
    }
}
