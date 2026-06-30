package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SurfaceObjectiveSacredApexTest {
    @Test fun `an apex-sacred world names the fallen divine beast`() {
        val s = PlanetSocietyState().also { it.apexKilled = true }
        val ctx = PlanetContext(PlanetTemperament.SILENT, SacredThing.APEX, PlanetStorySeed.NONE)
        assertTrue(SurfaceObjective.hudLine(PlanetBiome.NATURE, 0, s, ctx).contains("神獣"))
    }

    @Test fun `a non-sacred apex kill shows the plain disruption line`() {
        val s = PlanetSocietyState().also { it.apexKilled = true }
        assertTrue(SurfaceObjective.hudLine(PlanetBiome.NATURE, 0, s, PlanetContext.NEUTRAL).contains("揺ら"))
    }
}
