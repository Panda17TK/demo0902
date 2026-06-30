package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GentlePredatorRepelledImpactTest {
    @Test fun `a gentle world rewards driving a predator off a child more`() {
        val gentle = PlanetSocietyState().also {
            it.onPredatorRepelledNearChild(PlanetContext(PlanetTemperament.GENTLE, SacredThing.RUINS, PlanetStorySeed.NONE))
        }
        val neutral = PlanetSocietyState().also { it.onPredatorRepelledNearChild(PlanetContext.NEUTRAL) }
        assertTrue(gentle.mercy > neutral.mercy, "${gentle.mercy} vs ${neutral.mercy}")
    }
}
