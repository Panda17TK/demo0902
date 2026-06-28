package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.math.Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class FiringTest {
    @Test fun `single pellet with zero spread fires exactly at the aim angle`() {
        val angles = Firing.bulletAngles(aim = 1.0f, spread = 0f, pellets = 1, rng = Rng(1))
        assertEquals(1, angles.size); assertEquals(1.0f, angles[0], 1e-5f)
    }
    @Test fun `pellet count matches weapon pellets`() {
        val angles = Firing.bulletAngles(aim = 0f, spread = 0.25f, pellets = 6, rng = Rng(1))
        assertEquals(6, angles.size)
    }
    @Test fun `spread keeps angles within plus or minus spread of the aim`() {
        val angles = Firing.bulletAngles(aim = 0f, spread = 0.25f, pellets = 50, rng = Rng(7))
        angles.forEach { assertTrue(abs(it) <= 0.25f + 1e-4f, "angle $it outside spread") }
    }
}
