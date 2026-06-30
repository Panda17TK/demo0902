package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WorldStateTest {
    @Test fun `defaults to open space with no biome`() {
        val ws = WorldState()
        assertEquals(WorldMode.SPACE, ws.mode)
        assertNull(ws.biome)
        assertNull(ws.landingCandidate)
    }

    @Test fun `stores the planet biome of the surface being explored`() {
        val ws = WorldState(mode = WorldMode.SURFACE, biome = PlanetBiome.ICE)
        assertEquals(WorldMode.SURFACE, ws.mode)
        assertEquals(PlanetBiome.ICE, ws.biome)
    }

    @Test fun `a landing candidate keeps its planet biome`() {
        val ws = WorldState()
        ws.landingCandidate = PlanetBody(0f, 0f, 50f, 100f, 400f, PlanetBiome.GAS)
        assertEquals(PlanetBiome.GAS, ws.landingCandidate?.biome)
    }
}
