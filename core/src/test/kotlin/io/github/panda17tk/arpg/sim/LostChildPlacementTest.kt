package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LostChildPlacementTest {
    private fun ctx(s: PlanetStorySeed) = PlanetContext(PlanetTemperament.GENTLE, SacredThing.CHILDREN, s)
    private fun pop(s: PlanetStorySeed) =
        SurfaceEcology.populate(PlanetBiome.NATURE, 1000f, 1000f, 2000f, 2000f, Rng(1L), ctx(s))
    private fun count(soc: Society, key: String) = soc.placements.count { it.key == key }

    @Test fun `LOST_CHILD adds a strayed child, a stalking predator, and a guardian`() {
        val none = pop(PlanetStorySeed.NONE)
        val lost = pop(PlanetStorySeed.LOST_CHILD)
        // Same seed → identical base layout; the story seed appends exactly one of each (golden-path scene).
        assertEquals(count(none, "beast_whelp") + 1, count(lost, "beast_whelp"))
        assertEquals(count(none, "fang_wolf") + 1, count(lost, "fang_wolf"))
        assertEquals(count(none, "forest_guardian") + 1, count(lost, "forest_guardian"))
    }

    @Test fun `the strayed child is a pacifist, unlike the camp's young`() {
        assertTrue(pop(PlanetStorySeed.LOST_CHILD).placements.any { it.key == "beast_whelp" && it.passive }, "the lost child is passive")
        assertFalse(pop(PlanetStorySeed.NONE).placements.any { it.key == "beast_whelp" && it.passive }, "camp young are not passive")
    }
}
