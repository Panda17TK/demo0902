package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SurfaceGoalsTest {
    private val neutral = PlanetContext.NEUTRAL
    private val apexSacred = PlanetContext(PlanetTemperament.GENTLE, SacredThing.APEX, PlanetStorySeed.NONE)

    @Test fun `the same planet always derives the same goal set`() {
        for (biome in PlanetBiome.values()) {
            val ctx = PlanetContext.contextFor(99L, biome)
            assertEquals(SurfaceGoals.forPlanet(biome, ctx), SurfaceGoals.forPlanet(biome, ctx))
        }
    }

    @Test fun `nature always carries the protect-children goal`() {
        assertTrue(SurfaceGoalKind.PROTECT_CHILDREN in SurfaceGoals.forPlanet(PlanetBiome.NATURE, neutral))
    }

    @Test fun `children-sacred or lost-child planets protect children regardless of biome`() {
        val childSacred = PlanetContext(PlanetTemperament.PROUD, SacredThing.CHILDREN, PlanetStorySeed.NONE)
        assertTrue(SurfaceGoalKind.PROTECT_CHILDREN in SurfaceGoals.forPlanet(PlanetBiome.ICE, childSacred))
        val lostChild = PlanetContext(PlanetTemperament.PROUD, SacredThing.KING, PlanetStorySeed.LOST_CHILD)
        assertTrue(SurfaceGoalKind.PROTECT_CHILDREN in SurfaceGoals.forPlanet(PlanetBiome.ICE, lostChild))
    }

    @Test fun `the apex goal exists only where the ecology guarantees an apex`() {
        for (biome in PlanetBiome.values()) {
            val has = SurfaceGoalKind.DEFEAT_APEX in SurfaceGoals.forPlanet(biome, neutral)
            assertEquals(biome in SurfaceGoals.APEX_BIOMES, has, "apex goal mismatch on $biome")
        }
        // Pinned to SurfaceEcology's guaranteed WildRole.APEX placements (forest_apex / frost_worm).
        assertEquals(setOf(PlanetBiome.NATURE, PlanetBiome.ICE), SurfaceGoals.APEX_BIOMES)
    }

    @Test fun `harming a child fails the protection goal`() {
        val s = PlanetSocietyState(childKilled = true)
        assertEquals(GoalState.FAILED, SurfaceGoals.state(SurfaceGoalKind.PROTECT_CHILDREN, s, neutral, elitesAlive = 3))
    }

    @Test fun `the apex goal inverts on an apex-sacred world`() {
        val s = PlanetSocietyState(apexKilled = true)
        assertEquals(GoalState.FAILED, SurfaceGoals.state(SurfaceGoalKind.DEFEAT_APEX, s, apexSacred, 0))
        assertEquals(GoalState.DONE, SurfaceGoals.state(SurfaceGoalKind.DEFEAT_APEX, s, neutral, 0))
        assertEquals(GoalState.OPEN, SurfaceGoals.state(SurfaceGoalKind.DEFEAT_APEX, PlanetSocietyState(), apexSacred, 0))
    }

    @Test fun `defeat-masters needs both the leader down and no elites left`() {
        val led = PlanetSocietyState(leaderDefeated = true)
        assertEquals(GoalState.OPEN, SurfaceGoals.state(SurfaceGoalKind.DEFEAT_MASTERS, led, neutral, elitesAlive = 1))
        assertEquals(GoalState.DONE, SurfaceGoals.state(SurfaceGoalKind.DEFEAT_MASTERS, led, neutral, elitesAlive = 0))
    }

    @Test fun `chips cap at two, exclude the masters goal, and honour priority`() {
        val ctx = PlanetContext(PlanetTemperament.GENTLE, SacredThing.CHILDREN, PlanetStorySeed.NONE)
        val chips = SurfaceGoals.chipsFor(PlanetBiome.NATURE, ctx, PlanetSocietyState(), elitesAlive = 2)
        assertEquals(2, chips.size)
        assertTrue(chips[0].contains("子ら"), "protection chip first: $chips")
        val mastersChips = setOf(
            SurfaceGoals.chip(SurfaceGoalKind.DEFEAT_MASTERS, GoalState.OPEN, ctx),
            SurfaceGoals.chip(SurfaceGoalKind.DEFEAT_MASTERS, GoalState.DONE, ctx),
        )
        assertFalse(chips.any { it in mastersChips }, "masters goal stays off the chip row: $chips")
    }

    @Test fun `the taboo chip reads as a command`() {
        assertEquals("！ 神獣に触れるな", SurfaceGoals.chip(SurfaceGoalKind.DEFEAT_APEX, GoalState.OPEN, apexSacred))
    }

    @Test fun `every chip is non-blank in every state`() {
        for (kind in SurfaceGoalKind.values()) for (st in GoalState.values()) {
            assertTrue(SurfaceGoals.chip(kind, st, neutral).isNotBlank(), "blank chip for $kind/$st")
            assertTrue(SurfaceGoals.chip(kind, st, apexSacred).isNotBlank(), "blank chip for $kind/$st (apex-sacred)")
        }
    }
}
