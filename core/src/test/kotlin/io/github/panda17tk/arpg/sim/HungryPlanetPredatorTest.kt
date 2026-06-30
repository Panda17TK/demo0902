package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** A slain apex and a HUNGRY temperament both starve predators faster (the rule WildlifeSystem applies to hunger). */
class HungryPlanetPredatorTest {
    @Test fun `a slain apex leaves predators hungrier`() {
        assertTrue(SocietyTuning.wild(SocietyPressure(apexKilled = true)).hungerMul > 1f)
    }

    @Test fun `a hungry world's predators starve faster`() {
        assertTrue(SocietyTuning.wild(SocietyPressure(temperament = PlanetTemperament.HUNGRY)).hungerMul > 1f)
    }

    @Test fun `apex death and a hungry world compound`() {
        val both = SocietyTuning.wild(SocietyPressure(apexKilled = true, temperament = PlanetTemperament.HUNGRY)).hungerMul
        val apexOnly = SocietyTuning.wild(SocietyPressure(apexKilled = true)).hungerMul
        assertTrue(both > apexOnly)
    }

    @Test fun `a calm, sated world leaves hunger growth unchanged`() {
        assertEquals(1f, SocietyTuning.wild(SocietyPressure()).hungerMul, 1e-6f)
    }
}
