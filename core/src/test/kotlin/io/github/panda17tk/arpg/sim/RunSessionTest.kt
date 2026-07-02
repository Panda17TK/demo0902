package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RunSessionTest {
    private fun planet(id: Long = 7L, biome: PlanetBiome = PlanetBiome.NATURE) =
        PlanetBody(500f, 500f, 100f, 120f, 484f, biome, id, PlanetContext.contextFor(id, biome))

    @Test fun `a first visit is unknown and starts from an empty society`() {
        val plan = RunSession().planLanding(planet())
        assertFalse(plan.known)
        assertNull(plan.greeting)
        assertFalse(plan.showGreeting)
        assertEquals(0f, plan.society.hostility)
        assertFalse(plan.society.childKilled)
    }

    @Test fun `takeoff folds the visit into memory and the next landing recalls it`() {
        val session = RunSession()
        val p = planet()
        session.planLanding(p)
        val dirty = PlanetSocietyState(childKilled = true, hostility = 0.7f)
        session.completeTakeoff(dirty)
        val second = session.planLanding(p)
        assertTrue(second.known)
        assertTrue(second.society.childKilled)
        assertEquals(0.7f, second.society.hostility)
        assertEquals(SocietySpeechTrigger.ReturnVisitHostile, second.greeting)
        assertTrue(second.showGreeting)
    }

    @Test fun `each landing advances the surface seed monotonically`() {
        val session = RunSession()
        val s1 = session.planLanding(planet(id = 1L)).seed
        session.completeTakeoff(PlanetSocietyState())
        val s2 = session.planLanding(planet(id = 2L)).seed
        assertTrue(s2 > s1, "surface seed must advance: $s1 -> $s2")
    }

    @Test fun `takeoff clears the landed planet and returns the space seed and spawn`() {
        val session = RunSession()
        val p = planet()
        session.planLanding(p)
        val (seed, spawn) = session.completeTakeoff(PlanetSocietyState())
        assertNull(session.landedPlanetId)
        assertEquals(session.spaceSeed, seed)
        assertEquals(ReturnSpawn.beside(p), spawn)
    }

    @Test fun `reset restores a fresh run and forgets every planet`() {
        val session = RunSession()
        val p = planet()
        session.planLanding(p)
        session.completeTakeoff(PlanetSocietyState(hostility = 1f))
        session.reset()
        assertEquals(1L, session.spaceSeed)
        assertEquals(100L, session.surfSeed)
        assertNull(session.landedPlanetId)
        assertNull(session.returnSpawn)
        assertFalse(session.memory.knows(p.id))
    }
}
