package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.hypot

class ReturnSpawnTest {
    @Test fun `re-emerges just outside the planet it left`() {
        val planet = PlanetBody(100f, 200f, 50f, 100f, 400f, PlanetBiome.NATURE)
        val (x, y) = ReturnSpawn.beside(planet)
        assertEquals(planet.cx, x, 1e-4f, "stays on the planet's column")
        assertTrue(y < planet.cy - planet.radius, "should sit above the surface")
        assertTrue(hypot(x - planet.cx, y - planet.cy) > planet.radius, "should be outside the solid body")
    }
}
