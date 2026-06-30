package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.WorldMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WorldReturnTest {
    @Test fun `a surface has an escape pad at its landing point`() {
        val gw = WorldFactory.create(InputState(), seed = 5L, mode = WorldMode.SURFACE, biome = PlanetBiome.ICE)
        assertNotNull(gw.worldState.escapePad)
    }

    @Test fun `space has no escape pad`() {
        assertNull(WorldFactory.create(InputState(), seed = 5L).worldState.escapePad)
    }

    @Test fun `a player-spawn override places the player there`() {
        val gw = WorldFactory.create(InputState(), seed = 5L, playerSpawn = 1234f to 567f)
        val (x, y) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
        assertEquals(1234f, x, 1e-3f)
        assertEquals(567f, y, 1e-3f)
    }

    @Test fun `the same space seed reproduces the planet field (round-trip continuity)`() {
        val a = WorldFactory.create(InputState(), seed = 9L)
        val b = WorldFactory.create(InputState(), seed = 9L)
        assertEquals(a.planets.map { it.cx to it.cy }, b.planets.map { it.cx to it.cy })
    }
}
