package io.github.panda17tk.arpg.ecs.world

import com.github.quillraven.fleks.Entity
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.WorldMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.hypot

/** v2.36: space starts populated with coasting drifters that never gate the wave train. */
class WorldDrifterTest {
    @Test fun `space spawns 30 drifters, each with opening momentum`() {
        val gw = WorldFactory.create(InputState(), seed = 5L)
        var drifters = 0
        with(gw.world) {
            gw.world.family { all(Mob) }.forEach { e ->
                if (e[Mob].drifter) {
                    drifters++
                    val v = e[Velocity]
                    assertTrue(hypot(v.driftX, v.driftY) in 40f..140f, "drifters should coast below the ram threshold")
                }
            }
        }
        assertEquals(30, drifters)
    }

    @Test fun `surfaces have no drifters`() {
        val gw = WorldFactory.create(InputState(), seed = 5L, mode = WorldMode.SURFACE, biome = PlanetBiome.NATURE)
        var drifters = 0
        with(gw.world) { gw.world.family { all(Mob) }.forEach { e -> if (e[Mob].drifter) drifters++ } }
        assertEquals(0, drifters)
    }

    @Test fun `a wave completes even while drifters are still alive`() {
        val gw = WorldFactory.create(InputState(), seed = 5L)
        // Kill every wave mob; leave the drifters coasting out in the void.
        val toRemove = mutableListOf<Entity>()
        with(gw.world) { gw.world.family { all(Mob) }.forEach { e -> if (!e[Mob].drifter) toRemove.add(e) } }
        toRemove.forEach { gw.world -= it }
        gw.waveState.toSpawn = 0
        gw.waveState.elapsed = 999f
        gw.world.update(1f / 60f)
        assertEquals("intermission", gw.waveState.phase)
    }
}
