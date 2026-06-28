package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.config.PlayerConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class LocomotionTest {
    private val cfg = PlayerConfig()
    @Test fun `no keys means not moving`() {
        val mv = Locomotion.keyboardDirection(left = false, right = false, up = false, down = false)
        assertFalse(mv.isMoving)
        assertEquals(0f, mv.speedScale, 0f)
    }
    @Test fun `right only moves along plus x at full scale`() {
        val mv = Locomotion.keyboardDirection(false, true, false, false)
        assertEquals(1f, mv.dirX, 1e-5f); assertEquals(0f, mv.dirY, 1e-5f)
        assertEquals(1f, mv.speedScale, 0f); assertTrue(mv.isMoving)
    }
    @Test fun `up-right is normalized with y negative (y-down world)`() {
        val mv = Locomotion.keyboardDirection(false, true, true, false)
        val inv = 1f / sqrt(2f)
        assertEquals(inv, mv.dirX, 1e-5f); assertEquals(-inv, mv.dirY, 1e-5f)
    }
    @Test fun `dash requires held, moving, and stamina`() {
        assertTrue(Locomotion.isDashing(dashHeld = true, moving = true, sta = 50f))
        assertFalse(Locomotion.isDashing(dashHeld = true, moving = true, sta = 0f))
        assertFalse(Locomotion.isDashing(dashHeld = true, moving = false, sta = 50f))
        assertFalse(Locomotion.isDashing(dashHeld = false, moving = true, sta = 50f))
    }
    @Test fun `speed is 132 normally and 264 dashing`() {
        assertEquals(132f, Locomotion.speed(dashing = false, cfg = cfg), 1e-3f)
        assertEquals(264f, Locomotion.speed(dashing = true, cfg = cfg), 1e-3f)
    }
    @Test fun `stamina drains while dashing and clamps at zero`() {
        assertEquals(65f, Locomotion.nextStamina(100f, dashing = true, dt = 1f, cfg = cfg), 1e-3f)
        assertEquals(0f, Locomotion.nextStamina(10f, dashing = true, dt = 1f, cfg = cfg), 1e-3f)
    }
    @Test fun `stamina regens while not dashing and clamps at max`() {
        assertEquals(72f, Locomotion.nextStamina(50f, dashing = false, dt = 1f, cfg = cfg), 1e-3f)
        assertEquals(100f, Locomotion.nextStamina(95f, dashing = false, dt = 1f, cfg = cfg), 1e-3f)
    }
}
