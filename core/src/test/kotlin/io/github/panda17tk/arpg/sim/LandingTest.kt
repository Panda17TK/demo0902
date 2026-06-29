package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LandingTest {
    private val planet = PlanetBody(100f, 100f, 50f, 100f, 400f, PlanetBiome.DEAD)

    @Test fun `detects a planet when hovering over its surface`() {
        // distance 58 from centre ≤ radius 50 + range 16
        assertNotNull(Landing.nearestLandable(100f, 158f, listOf(planet), range = 16f))
    }

    @Test fun `no landing when far from any planet`() {
        assertNull(Landing.nearestLandable(400f, 400f, listOf(planet), 16f))
    }

    @Test fun `picks the nearest landable planet`() {
        val a = PlanetBody(0f, 0f, 40f, 100f, 400f, PlanetBiome.DEAD)
        val b = PlanetBody(120f, 0f, 40f, 100f, 400f, PlanetBiome.MAGMA)
        val near = Landing.nearestLandable(80f, 0f, listOf(a, b), 60f) // d(a)=80, d(b)=40 → b
        assertEquals(PlanetBiome.MAGMA, near?.biome)
    }
}
