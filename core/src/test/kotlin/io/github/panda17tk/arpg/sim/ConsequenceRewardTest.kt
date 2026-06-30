package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConsequenceRewardTest {
    @Test fun `a slain apex yields stronger material`() {
        assertTrue(Consequence.materialMultiplier(PlanetSocietyState().also { it.apexKilled = true }) > 1f)
    }

    @Test fun `an undisturbed apex yields a normal material`() {
        assertEquals(1f, Consequence.materialMultiplier(PlanetSocietyState()), 1e-6f)
    }

    @Test fun `the richer reward is paid for in ecological disruption`() {
        val s = PlanetSocietyState().also { it.onApexKilled() } // the deed records the cost…
        assertTrue(s.ecologicalDisruption > 0f)
        assertTrue(Consequence.materialMultiplier(s) > 1f)       // …and grants the stronger spoils
    }
}
