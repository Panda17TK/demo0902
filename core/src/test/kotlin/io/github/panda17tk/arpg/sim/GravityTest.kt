package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** How gravity acceleration feeds momentum (drift): scaled by response and a dash-escape multiplier. */
class GravityTest {
    @Test fun `gravity adds to drift toward the acceleration`() {
        val (dx, dy) = Gravity.applyToDrift(0f, 0f, ax = 10f, ay = 0f, response = 1f, dashMul = 1f, dt = 0.1f)
        assertTrue(dx > 0f, "dx was $dx")
        assertEquals(0f, dy, 1e-4f)
    }

    @Test fun `zero response ignores gravity`() {
        val (dx, dy) = Gravity.applyToDrift(5f, -3f, ax = 10f, ay = 20f, response = 0f, dashMul = 1f, dt = 0.1f)
        assertEquals(5f, dx, 1e-4f)
        assertEquals(-3f, dy, 1e-4f)
    }

    @Test fun `dash multiplier weakens gravity`() {
        val full = Gravity.applyToDrift(0f, 0f, 10f, 0f, response = 1f, dashMul = 1f, dt = 0.1f).first
        val dashed = Gravity.applyToDrift(0f, 0f, 10f, 0f, response = 1f, dashMul = 0.25f, dt = 0.1f).first
        assertTrue(dashed < full, "dashed=$dashed full=$full")
        assertEquals(full * 0.25f, dashed, 1e-4f)
    }

    @Test fun `heavier response pulls harder`() {
        val normal = Gravity.applyToDrift(0f, 0f, 10f, 0f, response = 1f, dashMul = 1f, dt = 0.1f).first
        val heavy = Gravity.applyToDrift(0f, 0f, 10f, 0f, response = 1.5f, dashMul = 1f, dt = 0.1f).first
        assertTrue(heavy > normal, "heavy=$heavy normal=$normal")
    }
}
