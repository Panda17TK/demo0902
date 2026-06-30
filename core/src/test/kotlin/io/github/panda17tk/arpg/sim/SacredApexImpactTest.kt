package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SacredApexImpactTest {
    private fun ctx(s: SacredThing) = PlanetContext(PlanetTemperament.SILENT, s, PlanetStorySeed.NONE)

    @Test fun `felling the apex shakes an apex-sacred world more`() {
        val sacred = PlanetSocietyState().also { it.onApexKilled(ctx(SacredThing.APEX)) }
        val plain = PlanetSocietyState().also { it.onApexKilled(ctx(SacredThing.RUINS)) }
        assertTrue(sacred.ecologicalDisruption > plain.ecologicalDisruption, "${sacred.ecologicalDisruption} vs ${plain.ecologicalDisruption}")
    }
}
