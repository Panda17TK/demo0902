package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Momentum integrator: accel adds velocity, light space-drag bleeds it slowly, speed is capped. */
class InertiaTest {
    @Test fun `acceleration increases velocity from rest`() {
        val (vx, vy) = Inertia.step(0f, 0f, ax = 100f, ay = 0f, decay = 0.5f, maxSpeed = 999f, dt = 0.1f)
        assertTrue(vx > 0f, "vx was $vx")
        assertEquals(0f, vy, 1e-4f)
    }

    @Test fun `momentum persists with light decay (space glide)`() {
        // No acceleration, light decay → keeps almost all velocity over one tick.
        val (vx, _) = Inertia.step(100f, 0f, ax = 0f, ay = 0f, decay = 0.9f, maxSpeed = 999f, dt = 0.1f)
        assertTrue(vx in 95f..99.9f, "vx was $vx — should retain most momentum")
    }

    @Test fun `heavier decay bleeds momentum faster than lighter decay`() {
        val (light, _) = Inertia.step(100f, 0f, 0f, 0f, decay = 0.9f, maxSpeed = 999f, dt = 0.2f)
        val (heavy, _) = Inertia.step(100f, 0f, 0f, 0f, decay = 0.3f, maxSpeed = 999f, dt = 0.2f)
        assertTrue(heavy < light, "heavy=$heavy light=$light")
    }

    @Test fun `speed is clamped to maxSpeed`() {
        val (vx, _) = Inertia.step(1000f, 0f, 0f, 0f, decay = 1f, maxSpeed = 150f, dt = 0.1f)
        assertEquals(150f, vx, 1f)
    }
}
