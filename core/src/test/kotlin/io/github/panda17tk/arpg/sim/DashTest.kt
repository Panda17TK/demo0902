package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.hypot

class DashTest {
    private val far = Tuning.TILE * 5f

    @Test fun `a non-dasher never dashes`() {
        assertFalse(Dash.ready(canDash = false, engaged = true, see = true, dist = far, cooldown = 0f))
    }

    @Test fun `an engaged dasher in range with its cooldown ready dashes`() {
        assertTrue(Dash.ready(canDash = true, engaged = true, see = true, dist = far, cooldown = 0f))
    }

    @Test fun `no dash while on cooldown`() {
        assertFalse(Dash.ready(true, engaged = true, see = true, dist = far, cooldown = 1f))
    }

    @Test fun `no dash when already on top of the target`() {
        assertFalse(Dash.ready(true, engaged = true, see = true, dist = 4f, cooldown = 0f))
    }

    @Test fun `no dash when not engaged or out of sight`() {
        assertFalse(Dash.ready(true, engaged = false, see = true, dist = far, cooldown = 0f))
        assertFalse(Dash.ready(true, engaged = true, see = false, dist = far, cooldown = 0f))
    }

    @Test fun `dash velocity points along the facing at dash speed`() {
        val (vx, vy) = Dash.velocity(1f, 0f)
        assertEquals(Dash.VELOCITY, vx, 1e-3f)
        assertEquals(0f, vy, 1e-3f)
    }

    @Test fun `dash velocity normalises the facing direction`() {
        val (vx, vy) = Dash.velocity(3f, 4f) // length 5
        assertEquals(Dash.VELOCITY * 0.6f, vx, 1e-3f)
        assertEquals(Dash.VELOCITY * 0.8f, vy, 1e-3f)
        assertEquals(Dash.VELOCITY, hypot(vx, vy), 1e-2f)
    }

    @Test fun `an undefined facing yields no dash velocity`() {
        val (vx, vy) = Dash.velocity(0f, 0f)
        assertEquals(0f, vx, 1e-6f)
        assertEquals(0f, vy, 1e-6f)
    }
}
