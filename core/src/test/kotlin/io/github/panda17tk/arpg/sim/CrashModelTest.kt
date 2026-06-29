package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CrashModelTest {
    @Test fun `no damage below the threshold`() {
        assertEquals(0f, CrashModel.damage(50f, threshold = 80f, k = 0.5f), 1e-4f)
    }

    @Test fun `damage scales with speed over the threshold`() {
        assertEquals(10f, CrashModel.damage(100f, threshold = 80f, k = 0.5f), 1e-4f) // 0.5*(100-80)
    }

    @Test fun `a harder impact does more damage`() {
        assertTrue(CrashModel.damage(200f, 80f, 0.5f) > CrashModel.damage(120f, 80f, 0.5f))
    }

    @Test fun `rebound turns inward motion outward`() {
        val (rx, _) = CrashModel.rebound(-100f, 0f, nx = 1f, ny = 0f, restitution = 0.3f)
        assertTrue(rx > 0f, "rx $rx should bounce outward (+x)")
    }

    @Test fun `rebound leaves outward motion unchanged`() {
        val (rx, ry) = CrashModel.rebound(100f, 0f, 1f, 0f, 0.3f)
        assertEquals(100f, rx, 1e-4f)
        assertEquals(0f, ry, 1e-4f)
    }
}
