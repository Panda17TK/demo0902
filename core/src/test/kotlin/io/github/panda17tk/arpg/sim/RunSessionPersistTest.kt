package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.save.InMemoryMemoryStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** LP v2.28: the fixed spaceSeed=1 universe persists across runs (and app restarts) via the store. */
class RunSessionPersistTest {
    private fun planet(id: Long = 7L) =
        PlanetBody(500f, 500f, 100f, 120f, 484f, PlanetBiome.NATURE, id, PlanetContext.contextFor(id, PlanetBiome.NATURE))

    @Test fun `a takeoff persists and a new session restores the memory`() {
        val store = InMemoryMemoryStore()
        val first = RunSession(store = store)
        first.planLanding(planet())
        first.completeTakeoff(PlanetSocietyState(childKilled = true, hostility = 0.7f))

        val next = RunSession(store = store) // a fresh app start
        next.restore()
        assertTrue(next.memory.knows(7L))
        assertEquals(0.7f, next.memory.recall(7L).hostility)
    }

    @Test fun `hostility survives a game over`() {
        val store = InMemoryMemoryStore()
        val session = RunSession(store = store)
        session.planLanding(planet())
        session.memory.remember(7L, PlanetSocietyState(hostility = 0.9f))
        session.persist() // the game-over checkpoint
        session.reset()   // 再挑戦 — with a store, the universe still remembers
        assertTrue(session.memory.knows(7L))
        val reloaded = RunSession(store = store).apply { restore() }
        assertEquals(0.9f, reloaded.memory.recall(7L).hostility)
    }

    @Test fun `without a store reset forgets everything (legacy behaviour)`() {
        val session = RunSession() // store = null
        session.planLanding(planet())
        session.completeTakeoff(PlanetSocietyState(hostility = 1f))
        session.reset()
        assertFalse(session.memory.knows(7L))
    }

    @Test fun `forgetting the universe clears memory and disk`() {
        val store = InMemoryMemoryStore()
        val session = RunSession(store = store)
        session.planLanding(planet())
        session.completeTakeoff(PlanetSocietyState(hostility = 1f))
        session.forgetUniverse()
        assertFalse(session.memory.knows(7L))
        assertEquals(null, store.stored)
        val reloaded = RunSession(store = store).apply { restore() }
        assertFalse(reloaded.memory.knows(7L))
    }

    @Test fun `a save from a different universe is ignored`() {
        val store = InMemoryMemoryStore()
        val other = RunSession(spaceSeed = 99L, store = store)
        other.planLanding(planet())
        other.completeTakeoff(PlanetSocietyState(hostility = 1f))
        val session = RunSession(store = store) // spaceSeed = 1
        session.restore()
        assertFalse(session.memory.knows(7L))
    }
}
