package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Ecological disruption makes the wild layer jumpier (the pure rule WildlifeSystem applies to fear). */
class WildlifeDisruptionTest {
    @Test fun `a shaken food web makes prey more fearful`() {
        val calm = SocietyTuning.wild(SocietyPressure(ecologicalDisruption = 0f)).fearMul
        val shaken = SocietyTuning.wild(SocietyPressure(ecologicalDisruption = 1f)).fearMul
        assertEquals(1f, calm, 1e-6f)
        assertTrue(shaken > calm)
    }

    @Test fun `fear scales with how shaken the web is`() {
        val a = SocietyTuning.wild(SocietyPressure(ecologicalDisruption = 0.3f)).fearMul
        val b = SocietyTuning.wild(SocietyPressure(ecologicalDisruption = 0.7f)).fearMul
        assertTrue(b > a)
    }
}
