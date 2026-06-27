package io.github.panda17tk.arpg.sim

import kotlin.math.sqrt

/** Resolved movement direction + analog scale (0 = not moving). */
data class MoveVector(val dirX: Float, val dirY: Float, val speedScale: Float) {
    val isMoving: Boolean get() = speedScale > 0f
    companion object { val NONE = MoveVector(0f, 0f, 0f) }
}

/**
 * Pure movement/dash/stamina logic, ported 1:1 from legacy combat.js updateCombat.
 * Kept free of libGDX/Fleks so it is fully unit-testable and deterministic.
 */
object Locomotion {
    /** Keyboard 8-direction, normalized, full speed. World is y-down (up = -1). */
    fun keyboardDirection(left: Boolean, right: Boolean, up: Boolean, down: Boolean): MoveVector {
        val ax = (if (left) -1f else 0f) + (if (right) 1f else 0f)
        val ay = (if (up) -1f else 0f) + (if (down) 1f else 0f)
        if (ax == 0f && ay == 0f) return MoveVector.NONE
        val len = sqrt(ax * ax + ay * ay)
        return MoveVector(ax / len, ay / len, 1f)
    }

    fun isDashing(dashHeld: Boolean, moving: Boolean, sta: Float): Boolean =
        dashHeld && moving && sta > 0f

    fun speed(dashing: Boolean): Float =
        Tuning.BASE_SPEED * Tuning.SPEED_MUL * (if (dashing) Tuning.DASH_MUL else 1f)

    fun nextStamina(sta: Float, dashing: Boolean, dt: Float): Float =
        if (dashing) (sta - Tuning.STA_DRAIN * dt).coerceAtLeast(0f)
        else (sta + Tuning.STA_REGEN * dt).coerceAtMost(Tuning.STA_MAX)
}
