package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HungryForestFoodWebTest {
    private fun ctx(s: PlanetStorySeed) = PlanetContext(PlanetTemperament.HUNGRY, SacredThing.APEX, s)
    private fun pop(s: PlanetStorySeed) =
        SurfaceEcology.populate(PlanetBiome.NATURE, 1000f, 1000f, 2000f, 2000f, Rng(2L), ctx(s))
    private val prey = setOf("horn_deer", "moss_hopper", "root_boar")
    private fun preyCount(soc: Society) = soc.placements.count { it.key in prey }
    private fun count(soc: Society, key: String) = soc.placements.count { it.key == key }

    @Test fun `a hungry forest has fewer prey and more predators`() {
        val none = pop(PlanetStorySeed.NONE)
        val hungry = pop(PlanetStorySeed.HUNGRY_FOREST)
        assertEquals(preyCount(none) - 2, preyCount(hungry), "two grazers culled")
        assertEquals(count(none, "fang_wolf") + 3, count(hungry, "fang_wolf"), "three more hunters")
    }

    @Test fun `the food web still has both predators and some prey left`() {
        val hungry = pop(PlanetStorySeed.HUNGRY_FOREST)
        assertTrue(preyCount(hungry) > 0)
        assertTrue(count(hungry, "fang_wolf") > 0)
    }
}
