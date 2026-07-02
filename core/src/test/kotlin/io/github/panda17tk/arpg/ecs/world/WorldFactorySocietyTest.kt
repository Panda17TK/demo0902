package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.PlanetSocietyState
import io.github.panda17tk.arpg.sim.WorldMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** R2: the remembered society is injected BEFORE the world is built, so spawn-time code can read it. */
class WorldFactorySocietyTest {
    @Test fun `an injected society is live from construction`() {
        val remembered = PlanetSocietyState(hostility = 0.7f, childKilled = true)
        val gw = WorldFactory.create(
            InputState(), seed = 5L, mode = WorldMode.SURFACE, biome = PlanetBiome.NATURE,
            society = remembered,
        )
        assertEquals(0.7f, gw.worldState.society.hostility)
        assertTrue(gw.worldState.society.childKilled)
    }

    @Test fun `no society means the usual fresh state`() {
        val gw = WorldFactory.create(InputState(), seed = 5L, mode = WorldMode.SURFACE, biome = PlanetBiome.NATURE)
        assertEquals(0f, gw.worldState.society.hostility)
        assertFalse(gw.worldState.society.childKilled)
    }
}
