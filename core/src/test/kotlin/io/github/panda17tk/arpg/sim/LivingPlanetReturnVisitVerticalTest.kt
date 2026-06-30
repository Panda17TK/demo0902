package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * One deterministic vertical slice of Living Planets: land a LOST_CHILD nature world, save the child,
 * take off, return — and confirm the planet remembers and greets the player by that deed.
 */
class LivingPlanetReturnVisitVerticalTest {
    @Test fun `saving a lost child on a nature world is remembered and greeted on return`() {
        val book = PlanetMemoryBook()
        val id = Planets.idFor(42L, 0)
        val ctx = PlanetContext(PlanetTemperament.GENTLE, SacredThing.CHILDREN, PlanetStorySeed.LOST_CHILD)

        // 1. The surface spawns with the story's child, a stalking predator, and a guardian.
        val keys = SurfaceEcology.populate(PlanetBiome.NATURE, 1000f, 1000f, 2000f, 2000f, Rng(1L), ctx)
            .placements.map { it.key }.toSet()
        assertTrue("beast_whelp" in keys, "no child")
        assertTrue("fang_wolf" in keys, "no predator")
        assertTrue("forest_guardian" in keys, "no guardian")

        // 2. First visit: seed from (blank) memory, the player drives the predator off the child.
        val visit = book.recall(id)
        visit.onPredatorRepelledNearChild(ctx)
        assertTrue(visit.mercy > 0f)

        // 3. Takeoff folds the visit into memory.
        book.remember(id, visit)

        // 4. Returning, the planet remembers the kindness…
        val ret = book.recall(id)
        assertTrue(ret.predatorKilledNearChild && ret.mercy > 0f)

        // 5. …and greets the player warmly, in both the speech greeting and the objective HUD.
        assertEquals(SocietySpeechTrigger.ReturnVisitMerciful, SocietySpeechLines.returnGreeting(ret))
        assertTrue(SurfaceObjective.hudLine(PlanetBiome.NATURE, 0, ret, ctx, remembered = true).contains("借り"))
    }
}
