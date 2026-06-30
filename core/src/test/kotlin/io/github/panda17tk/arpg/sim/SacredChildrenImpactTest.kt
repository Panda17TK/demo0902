package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SacredChildrenImpactTest {
    private fun ctx(s: SacredThing) = PlanetContext(PlanetTemperament.SILENT, s, PlanetStorySeed.NONE)

    @Test fun `harming a child cuts deeper where children are sacred`() {
        val sacred = PlanetSocietyState().also { it.onChildHarmed(ctx(SacredThing.CHILDREN)) }
        val plain = PlanetSocietyState().also { it.onChildHarmed(ctx(SacredThing.RUINS)) }
        assertTrue(sacred.hostility > plain.hostility, "${sacred.hostility} vs ${plain.hostility}")
    }
}
