package io.github.panda17tk.arpg.sim

import kotlin.math.hypot

/**
 * Enemy dash: a thrust in the facing direction that becomes the initial velocity of inertial drift.
 * About half of each tribe's rank-and-file dash (assigned at spawn). The dash is used to close a gap
 * and to control attitude — it sets the creature's drift velocity along its facing, which then coasts
 * and decays like any other momentum. Pure.
 */
object Dash {
    const val VELOCITY = 300f // speed of the dash burst (a few × walking speed for a moment)
    const val COOLDOWN = 2.0f // seconds between dashes
    val MIN_DIST = Tuning.TILE * 3f // only dash to close a gap, not when already on top of the target

    /** Whether a creature should fire a dash this tick. */
    fun ready(canDash: Boolean, engaged: Boolean, see: Boolean, dist: Float, cooldown: Float): Boolean =
        canDash && engaged && see && cooldown <= 0f && dist > MIN_DIST

    /** The dash velocity along the (unit-normalised) facing direction; zero if facing is undefined. */
    fun velocity(faceX: Float, faceY: Float, speed: Float = VELOCITY): Pair<Float, Float> {
        val l = hypot(faceX, faceY)
        return if (l < 1e-4f) 0f to 0f else (faceX / l * speed) to (faceY / l * speed)
    }
}
