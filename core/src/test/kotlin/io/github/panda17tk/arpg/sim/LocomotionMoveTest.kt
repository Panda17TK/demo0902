package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Acceleration-based movement: input/dash accelerate, friction decelerates, speed is capped. */
class LocomotionMoveTest {
    @Test fun `accelerates from rest toward the input direction`() {
        val (vx, _) = Locomotion.applyMove(0f, 0f, 1f, 0f, moving = true, accel = 500f, friction = 0.5f, maxSpeed = 200f, dt = 0.1f)
        assertTrue(vx > 0f)
    }

    @Test fun `clamps speed to the maximum`() {
        val (vx, _) = Locomotion.applyMove(1000f, 0f, 1f, 0f, moving = true, accel = 500f, friction = 0.5f, maxSpeed = 150f, dt = 0.1f)
        assertEquals(150f, vx, 1f)
    }

    @Test fun `friction bleeds off velocity when not moving`() {
        val (vx, _) = Locomotion.applyMove(100f, 0f, 0f, 0f, moving = false, accel = 500f, friction = 0.1f, maxSpeed = 200f, dt = 0.1f)
        assertTrue(vx in 1f..99f, "vx was $vx")
    }
}
