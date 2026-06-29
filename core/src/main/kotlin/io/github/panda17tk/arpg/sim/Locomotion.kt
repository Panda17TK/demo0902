package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.config.PlayerConfig
import kotlin.math.pow
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

    fun speed(dashing: Boolean, cfg: PlayerConfig): Float =
        cfg.baseSpeed * cfg.speedMul * (if (dashing) cfg.dashMul else 1f)

    fun nextStamina(sta: Float, dashing: Boolean, dt: Float, cfg: PlayerConfig): Float =
        if (dashing) (sta - cfg.staDrain * dt).coerceAtLeast(0f)
        else (sta + cfg.staRegen * dt).coerceAtMost(cfg.staMax)

    /**
     * Acceleration-based movement. While [moving], push acceleration along (dirX,dirY); always apply
     * [friction] (heavier when stopped), then clamp the result to [maxSpeed]. Gives a weighty ramp-up
     * and coast instead of instant top speed.
     */
    fun applyMove(vx: Float, vy: Float, dirX: Float, dirY: Float, moving: Boolean, accel: Float, friction: Float, maxSpeed: Float, dt: Float): Pair<Float, Float> {
        var nx = vx; var ny = vy
        if (moving) { nx += dirX * accel * dt; ny += dirY * accel * dt }
        val f = friction.pow(dt)
        nx *= f; ny *= f
        val sp = sqrt(nx * nx + ny * ny)
        if (sp > maxSpeed && sp > 1e-4f) { nx = nx / sp * maxSpeed; ny = ny / sp * maxSpeed }
        return nx to ny
    }
}
