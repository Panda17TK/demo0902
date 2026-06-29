package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.map.Biome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CircleCollisionTest {
    private val planet = PlanetBody(0f, 0f, 50f, 100f, 400f, Biome.ROCK)

    @Test fun `no collision when outside the planet`() {
        val r = CircleCollision.resolve(200f, 0f, 10f, -100f, 0f, listOf(planet))
        assertFalse(r.hit)
        assertEquals(200f, r.x, 1e-4f)
    }

    @Test fun `pushes the body out to the surface`() {
        // Inside: distance 40 < radius 50 + body 10.
        val r = CircleCollision.resolve(40f, 0f, 10f, 0f, 0f, listOf(planet))
        assertTrue(r.hit)
        assertEquals(60f, r.x, 1e-3f) // pushed to radius + bodyR on +x
        assertEquals(0f, r.y, 1e-3f)
    }

    @Test fun `reports inward speed for a body driving into the planet`() {
        val r = CircleCollision.resolve(40f, 0f, 10f, -200f, 0f, listOf(planet)) // moving toward centre
        assertTrue(r.inwardSpeed > 0f, "inward ${r.inwardSpeed}")
    }

    @Test fun `tangential motion has no inward speed`() {
        val r = CircleCollision.resolve(40f, 0f, 10f, 0f, 200f, listOf(planet)) // moving +y, normal is +x
        assertEquals(0f, r.inwardSpeed, 1e-3f)
    }
}
