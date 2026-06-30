package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.WorldMode
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** A nature surface must carry BOTH populations: the sapient society and the wild ecosystem. */
class WorldWildlifeTest {
    private fun countByKind(gw: GameWorld): Map<LifeKind, Int> {
        val counts = HashMap<LifeKind, Int>()
        gw.world.family { all(Mob) }.forEach { e ->
            val kind = with(gw.world) { e[Mob].def.lifeKind }
            counts[kind] = (counts[kind] ?: 0) + 1
        }
        return counts
    }

    @Test fun `a nature surface spawns both a sapient society and wildlife`() {
        val gw = WorldFactory.create(InputState(), seed = 3L, mode = WorldMode.SURFACE, biome = PlanetBiome.NATURE)
        val counts = countByKind(gw)
        assertTrue((counts[LifeKind.SAPIENT] ?: 0) > 0, "expected sapient society members, got $counts")
        assertTrue((counts[LifeKind.WILDLIFE] ?: 0) > 0, "expected wildlife, got $counts")
    }

    @Test fun `wildlife survive a minute of ticking without faulting`() {
        // WildlifeSystem (not the hostile AI) drives wildlife; ticking exercises its sensing + movement paths.
        val gw = WorldFactory.create(InputState(), seed = 5L, mode = WorldMode.SURFACE, biome = PlanetBiome.NATURE)
        repeat(60) { gw.world.update(1f / 60f) }
        assertTrue((countByKind(gw)[LifeKind.WILDLIFE] ?: 0) > 0, "wildlife should persist on the surface")
    }
}
