package io.github.panda17tk.arpg.input

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.hypot

/** v2.188 ゲームパッド: the pure stick/trigger mapping. */
class GamepadMapTest {
    @Test fun `a centered stick is inactive`() {
        val s = GamepadMap.stick(0.1f, -0.1f, GamepadMap.MOVE_DEAD)
        assertFalse(s.active)
        assertEquals(0f, s.mag, 0.001f)
    }

    @Test fun `a fully pushed stick is a unit vector at full magnitude`() {
        val s = GamepadMap.stick(1f, 0f, GamepadMap.MOVE_DEAD)
        assertTrue(s.active)
        assertEquals(1f, hypot(s.x, s.y), 0.001f)
        assertEquals(1f, s.mag, 0.001f)
    }

    @Test fun `magnitude renormalizes from the deadzone edge`() {
        val d = 0.25f
        assertFalse(GamepadMap.stick(d, 0f, d).active, "exactly at the ring is still inside")
        val half = GamepadMap.stick(0.625f, 0f, d) // halfway from ring(0.25) to rim(1.0)
        assertTrue(half.active)
        assertEquals(0.5f, half.mag, 0.01f)
        assertEquals(1f, half.x, 0.001f) // direction is still a unit vector
    }

    @Test fun `the trigger threshold is a firm half-pull`() {
        assertFalse(GamepadMap.pressed(0.49f))
        assertTrue(GamepadMap.pressed(0.5f))
        assertTrue(GamepadMap.pressed(1f))
    }
}
