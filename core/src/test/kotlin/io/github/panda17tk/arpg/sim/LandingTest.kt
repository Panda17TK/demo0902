package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.map.Biome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LandingTest {
    private val planet = PlanetBody(100f, 100f, 50f, 100f, 400f, Biome.ROCK)

    @Test fun `detects a planet when hovering over its surface`() {
        // distance 58 from centre ≤ radius 50 + range 16
        assertNotNull(Landing.nearestLandable(100f, 158f, listOf(planet), range = 16f))
    }

    @Test fun `no landing when far from any planet`() {
        assertNull(Landing.nearestLandable(400f, 400f, listOf(planet), 16f))
    }

    @Test fun `picks the nearest landable planet`() {
        val a = PlanetBody(0f, 0f, 40f, 100f, 400f, Biome.ROCK)
        val b = PlanetBody(120f, 0f, 40f, 100f, 400f, Biome.MAGMA)
        val near = Landing.nearestLandable(80f, 0f, listOf(a, b), 60f) // d(a)=80, d(b)=40 → b
        assertEquals(Biome.MAGMA, near?.biome)
    }
}
