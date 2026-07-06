package io.github.panda17tk.arpg.audio

/**
 * v2.89: what the ambient bed should do when time itself is being held — slow-mo and the
 * boss-kill white-out pull the volume floor down; releasing eases it back up. Pure math.
 */
object AudioDuck {
    const val FLOOR = 0.35f

    /** The duck target this frame given the feel state. */
    fun target(timeHeld: Boolean, flashAlpha: Float): Float =
        if (timeHeld || flashAlpha > 0.3f) FLOOR else 1f

    /** One easing step of the running duck value toward [target] (fast down, slower up). */
    fun step(current: Float, target: Float, dt: Float): Float {
        val rate = if (target < current) 14f else 4f
        return current + (target - current) * (1f - kotlin.math.exp(-rate * dt))
    }
}
