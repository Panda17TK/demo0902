package io.github.panda17tk.arpg.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EnemyDefTest {
    @Test fun `gravityResponse defaults to normal (1)`() {
        assertEquals(1f, EnemyDef(name = "x", hp = 10f, speed = 10f).gravityResponse, 1e-6f)
    }

    @Test fun `default catalog assigns varied gravity responses`() {
        val e = GameConfig().enemies
        assertEquals(0f, e.getValue("stalker").gravityResponse, 1e-6f)   // ignores gravity
        assertEquals(1.5f, e.getValue("brute").gravityResponse, 1e-6f)   // heavy — easy to fling
        assertEquals(0.25f, e.getValue("overlord").gravityResponse, 1e-6f) // boss resists
        assertEquals(1f, e.getValue("zombie").gravityResponse, 1e-6f)    // default
    }
}
