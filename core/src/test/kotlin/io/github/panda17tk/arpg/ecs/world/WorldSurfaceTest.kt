package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.WorldMode
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Integration smoke tests for the surface path: ecology placement + the social AI ticking on real creatures. */
class WorldSurfaceTest {
    @Test fun `landing on a nature planet spawns its society`() {
        val gw = WorldFactory.create(InputState(), seed = 3L, mode = WorldMode.SURFACE, biome = PlanetBiome.NATURE)
        assertTrue(gw.world.family { all(Mob) }.numEntities > 0, "ecology should place inhabitants")
    }

    @Test fun `every biome surface builds and ticks without error`() {
        // Exercises SurfaceEcology placement + AISystem (warn/rally/protect/surrender/ignore) + movement on
        // biome creatures. A runtime fault in any of the new social-AI paths would throw here.
        for (b in PlanetBiome.values()) {
            val gw = WorldFactory.create(InputState(), seed = 7L, mode = WorldMode.SURFACE, biome = b)
            repeat(30) { gw.world.update(1f / 60f) }
            assertTrue(gw.world.family { all(Mob) }.numEntities >= 0) // reached here = no exception thrown
        }
    }
}
