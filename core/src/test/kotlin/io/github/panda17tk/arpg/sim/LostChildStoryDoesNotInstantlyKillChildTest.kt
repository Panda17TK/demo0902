package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.hypot

class LostChildStoryDoesNotInstantlyKillChildTest {
    @Test fun `the strayed child is not spawned on top of its stalking predator`() {
        val ctx = PlanetContext(PlanetTemperament.GENTLE, SacredThing.CHILDREN, PlanetStorySeed.LOST_CHILD)
        val soc = SurfaceEcology.populate(PlanetBiome.NATURE, 1000f, 1000f, 2000f, 2000f, Rng(1L), ctx)

        // The strayed child is the LOST_CHILD beast_whelp — added last, and passive (unlike the camp's young).
        val child = soc.placements.last { it.key == "beast_whelp" }
        assertTrue(child.passive, "the strayed child is a pacifist")

        // No predator should be spawned right on top of it, so predation can't trigger on the first tick.
        val nearest = soc.placements.filter { it.key == "fang_wolf" }.minOf { hypot(it.x - child.x, it.y - child.y) }
        assertTrue(nearest >= Tuning.TILE, "a predator must not start atop the child (nearest=$nearest)")
    }
}
