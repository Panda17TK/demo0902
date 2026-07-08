package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.map.SurfaceWater
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.SurfaceEcology
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.sim.WorldMode
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.hypot

/** v2.133 適所の生態, live-world half: the spawned surface honours water — otters on the bank, deer on dry land. */
class HabitatWorldTest {
    @Test fun `on a wet world the land animals start dry and the otters start by the water`() {
        var checked = false
        for (seed in 1L..30L) {
            val gw = WorldFactory.create(InputState(), seed = seed, mode = WorldMode.SURFACE, biome = PlanetBiome.NATURE)
            val water = gw.worldState.water
            if (water.isEmpty || water.frozen) continue
            checked = true
            var otters = 0
            with(gw.world) {
                gw.world.family { all(Mob, Transform) }.forEach { e ->
                    val m = e[Mob]; val t = e[Transform]
                    if (m.def.lifeKind != LifeKind.WILDLIFE) return@forEach
                    if (m.def.id in SurfaceEcology.WATERSIDE) {
                        otters++
                        val shore = SurfaceWater.nearestShore(water, t.x, t.y)!!
                        assertTrue(hypot(shore.first - t.x, shore.second - t.y) < Tuning.TILE * 4f,
                            "seed $seed: ${m.def.id} lives by the water (${hypot(shore.first - t.x, shore.second - t.y)})")
                    } else {
                        assertTrue(!SurfaceWater.wadingAt(water, t.x, t.y),
                            "seed $seed: ${m.def.id} must not start standing in open water")
                    }
                }
            }
            assertTrue(otters > 0, "seed $seed: the wet world keeps its waterside animals")
            break // one wet world proves the pass
        }
        assertTrue(checked, "no wet NATURE world in seeds 1..30")
    }
}
