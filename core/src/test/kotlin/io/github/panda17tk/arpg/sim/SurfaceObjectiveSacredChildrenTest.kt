package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SurfaceObjectiveSacredChildrenTest {
    @Test fun `a children-sacred world words child-harm as sacrilege`() {
        val s = PlanetSocietyState().also { it.childHarmed = true }
        val ctx = PlanetContext(PlanetTemperament.SILENT, SacredThing.CHILDREN, PlanetStorySeed.NONE)
        assertTrue(SurfaceObjective.hudLine(PlanetBiome.NATURE, 0, s, ctx).contains("星は怒っている"))
    }

    @Test fun `a non-children-sacred world uses the plain child-harm line`() {
        val s = PlanetSocietyState().also { it.childHarmed = true }
        assertTrue(SurfaceObjective.hudLine(PlanetBiome.NATURE, 0, s, PlanetContext.NEUTRAL).contains("守護者が奮い立つ"))
    }
}
