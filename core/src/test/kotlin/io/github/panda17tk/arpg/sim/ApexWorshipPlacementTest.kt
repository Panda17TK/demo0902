package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ApexWorshipPlacementTest {
    private fun ctx(s: PlanetStorySeed) = PlanetContext(PlanetTemperament.RITUALISTIC, SacredThing.APEX, s)
    private fun pop(s: PlanetStorySeed) =
        SurfaceEcology.populate(PlanetBiome.NATURE, 1000f, 1000f, 2000f, 2000f, Rng(3L), ctx(s))
    private fun count(soc: Society, key: String) = soc.placements.count { it.key == key }

    @Test fun `APEX_WORSHIP places one more far-roaming apex`() {
        assertEquals(count(pop(PlanetStorySeed.NONE), "forest_apex") + 1, count(pop(PlanetStorySeed.APEX_WORSHIP), "forest_apex"))
    }

    @Test fun `the worshipped apex is placed as a wild, roaming beast`() {
        val apexes = pop(PlanetStorySeed.APEX_WORSHIP).placements.filter { it.key == "forest_apex" }
        assertTrue(apexes.size >= 2, "base apex plus the worshipped one")
        assertTrue(apexes.all { it.passive }, "apex placed as wildlife, not a camp combatant")
    }
}
