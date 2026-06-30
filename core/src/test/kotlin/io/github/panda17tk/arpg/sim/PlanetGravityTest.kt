package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class PlanetGravityTest {
    @Test fun `pulls toward a planet within range`() {
        val ps = listOf(PlanetBody(0f, 0f, 50f, 100f, 400f, PlanetBiome.DEAD))
        val (ax, ay) = PlanetGravity.gravityAccelAt(ps, 100f, 0f)
        assertTrue(ax < 0f, "ax $ax should pull left toward the planet")
        assertEquals(0f, ay, 1e-3f)
    }

    @Test fun `no pull beyond gravity range`() {
        val ps = listOf(PlanetBody(0f, 0f, 50f, 100f, 200f, PlanetBiome.DEAD))
        val (ax, ay) = PlanetGravity.gravityAccelAt(ps, 500f, 0f)
        assertEquals(0f, ax, 1e-4f)
        assertEquals(0f, ay, 1e-4f)
    }

    @Test fun `heavier planet pulls harder at the same distance`() {
        val light = PlanetGravity.gravityAccelAt(listOf(PlanetBody(0f, 0f, 50f, 100f, 400f, PlanetBiome.DEAD)), 100f, 0f).first
        val heavy = PlanetGravity.gravityAccelAt(listOf(PlanetBody(0f, 0f, 50f, 300f, 400f, PlanetBiome.DEAD)), 100f, 0f).first
        assertTrue(abs(heavy) > abs(light), "heavy=$heavy light=$light")
    }

    @Test fun `combined adds planet and cluster pull`() {
        val ps = listOf(PlanetBody(0f, 0f, 50f, 100f, 400f, PlanetBiome.DEAD))
        val cs = listOf(Cluster(0f, 0f, 3f, 25))
        val (ax, _) = PlanetGravity.combinedGravityAccel(ps, cs, 100f, 0f, 400f, 50f)
        val planetOnly = PlanetGravity.gravityAccelAt(ps, 100f, 0f).first
        assertTrue(ax < planetOnly, "combined $ax should be stronger (more negative) than planet-only $planetOnly")
    }
}
