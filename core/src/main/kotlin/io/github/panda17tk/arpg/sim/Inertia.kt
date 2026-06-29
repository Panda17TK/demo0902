package io.github.panda17tk.arpg.sim

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Pure momentum integrator shared by the player and mobs. Adds acceleration to velocity, applies a
 * light "space" drag (decay^dt) so momentum persists across frames, then clamps to maxSpeed.
 * Kept free of libGDX/Fleks for deterministic unit testing.
 */
object Inertia {
    fun step(
        vx: Float, vy: Float, ax: Float, ay: Float, decay: Float, maxSpeed: Float, dt: Float,
    ): Pair<Float, Float> {
        var nx = vx + ax * dt
        var ny = vy + ay * dt
        val d = decay.pow(dt)
        nx *= d; ny *= d
        val sp = sqrt(nx * nx + ny * ny)
        if (sp > maxSpeed && sp > 1e-4f) { nx = nx / sp * maxSpeed; ny = ny / sp * maxSpeed }
        return nx to ny
    }
}
