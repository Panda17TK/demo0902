package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class PlanetStorySeedSurfaceEcologyTest {
    private fun ctx(s: PlanetStorySeed) = PlanetContext(PlanetTemperament.GENTLE, SacredThing.CHILDREN, s)
    private fun pop(seed: Long, s: PlanetStorySeed) =
        SurfaceEcology.populate(PlanetBiome.NATURE, 1000f, 1000f, 2000f, 2000f, Rng(seed), ctx(s))

    @Test fun `populate is deterministic for the same seed and context`() {
        val a = pop(7L, PlanetStorySeed.LOST_CHILD); val b = pop(7L, PlanetStorySeed.LOST_CHILD)
        assertEquals(a.placements.map { it.key }, b.placements.map { it.key })
    }

    @Test fun `a story seed changes the ecology from the plain layout`() {
        assertNotEquals(pop(7L, PlanetStorySeed.NONE).placements.size, pop(7L, PlanetStorySeed.LOST_CHILD).placements.size)
    }

    @Test fun `NONE (and the default context) leaves the base layout untouched`() {
        val withNone = pop(7L, PlanetStorySeed.NONE)
        val noCtx = SurfaceEcology.populate(PlanetBiome.NATURE, 1000f, 1000f, 2000f, 2000f, Rng(7L))
        assertEquals(noCtx.placements.map { it.key }, withNone.placements.map { it.key })
    }
}
