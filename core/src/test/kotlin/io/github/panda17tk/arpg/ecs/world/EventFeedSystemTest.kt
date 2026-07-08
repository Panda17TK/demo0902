package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.EventKind
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.sim.WorldMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EventFeedSystemTest {
    private val dt = 1f / 60f

    private fun surfaceWorld() = WorldFactory.create(
        InputState(), seed = 5L, mode = WorldMode.SURFACE, biome = PlanetBiome.NATURE,
    )

    @Test fun `a society edge lands in the feed one tick later`() {
        val gw = surfaceWorld()
        gw.world.update(dt) // baseline snapshot
        assertTrue(gw.worldState.recentEvents.isEmpty(), "no events before any deed")
        gw.worldState.society.onChildKilled()
        gw.world.update(dt)
        val events = gw.worldState.recentEvents
        assertTrue(events.isNotEmpty(), "child kill must reach the feed")
        assertEquals(EventKind.HOSTILE, events[0].kind)
    }

    @Test fun `a repeated per-tick emitter fires the feed only once`() {
        // v2.126: bigger surfaces let the live ecology feed organic lines early, so this test
        // compares against an identically-advanced control world — only the emitter's effect shows.
        val gw = surfaceWorld(); val control = surfaceWorld()
        gw.world.update(dt); control.world.update(dt)
        repeat(30) { // this emitter is called every tick while the condition holds
            gw.worldState.society.onWildPredatorThreatenedChild()
            gw.world.update(dt); control.world.update(dt)
        }
        assertEquals(control.worldState.recentEvents.size, gw.worldState.recentEvents.size,
            "the per-tick threat flag has no feed line")
        // A boolean deed reported repeatedly stays a single edge. leaderDefeated is used here
        // because the threat gauge above can organically latch the child-harm edge in gw.
        gw.worldState.society.leaderDefeated = true
        gw.world.update(dt); control.world.update(dt)
        gw.worldState.society.leaderDefeated = true // still up: no new edge
        gw.world.update(dt); control.world.update(dt)
        assertEquals(control.worldState.recentEvents.size + 1, gw.worldState.recentEvents.size)
    }

    @Test fun `lines expire after their lifetime`() {
        val gw = surfaceWorld()
        gw.world.update(dt)
        gw.worldState.society.onRelicClaimed()
        gw.world.update(dt)
        val relicLine = gw.worldState.recentEvents.firstOrNull()
        assertTrue(relicLine != null, "relic claim must reach the feed")
        val ticks = (Tuning.EVENT_FEED_LIFE / dt).toInt() + 2
        repeat(ticks) { gw.world.update(dt) }
        // The live ecology may organically add NEW lines meanwhile — assert the old one is gone
        // and nothing still shown has outlived the lifetime.
        assertTrue(gw.worldState.recentEvents.none { it === relicLine }, "the relic line must expire")
        assertTrue(gw.worldState.recentEvents.all { it.age < Tuning.EVENT_FEED_LIFE }, "no line may outlive its lifetime")
    }

    @Test fun `space worlds never feed`() {
        val gw = WorldFactory.create(InputState(), seed = 1L) // SPACE
        gw.world.update(dt)
        gw.worldState.society.onChildKilled()
        gw.world.update(dt)
        assertTrue(gw.worldState.recentEvents.isEmpty())
    }

    @Test fun `the feed never grows past its cap`() {
        val gw = surfaceWorld()
        gw.world.update(dt)
        // land 6 distinct edges over separate ticks
        gw.worldState.society.onChildKilled(); gw.world.update(dt)
        gw.worldState.society.onApexKilled(); gw.world.update(dt)
        gw.worldState.society.onHatchlingKilled(); gw.world.update(dt)
        gw.worldState.society.onNestMotherKilled(); gw.world.update(dt)
        gw.worldState.society.onRelicClaimed(); gw.world.update(dt)
        gw.worldState.society.leaderDefeated = true; gw.world.update(dt)
        assertTrue(gw.worldState.recentEvents.size <= Tuning.EVENT_FEED_MAX, "feed exceeded its cap: ${gw.worldState.recentEvents.size}")
    }
}
