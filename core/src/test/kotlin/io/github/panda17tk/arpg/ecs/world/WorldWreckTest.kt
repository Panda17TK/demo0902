package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Pickup
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.WorldMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.hypot

/** v2.46 難破船: every system scatters 2..3 wrecks with a weapon cache at each site. */
class WorldWreckTest {
    @Test fun `space places two or three wrecks, deterministically`() {
        val a = WorldFactory.create(InputState(), seed = 11L)
        val b = WorldFactory.create(InputState(), seed = 11L)
        assertTrue(a.worldState.wrecks.size in 2..3, "got ${a.worldState.wrecks.size}")
        assertEquals(a.worldState.wrecks, b.worldState.wrecks)
    }

    @Test fun `surfaces have no wrecks`() {
        val gw = WorldFactory.create(InputState(), seed = 3L, mode = WorldMode.SURFACE, biome = PlanetBiome.NATURE)
        assertTrue(gw.worldState.wrecks.isEmpty())
    }

    @Test fun `every wreck holds a weapon cache and a dust bundle`() {
        val gw = WorldFactory.create(InputState(), seed = 11L)
        for ((wx, wy) in gw.worldState.wrecks) {
            var gun = 0; var dust = 0
            with(gw.world) {
                gw.world.family { all(Pickup, Transform) }.forEach { e ->
                    val t = e[Transform]
                    if (hypot(t.x - wx, t.y - wy) > 60f) return@forEach
                    val k = e[Pickup].kind
                    if (k.startsWith("item:")) gun++
                    if (k == "dust") dust += e[Pickup].amount
                }
            }
            assertTrue(gun >= 1, "a wreck at ($wx,$wy) must hold a weapon cache")
            assertTrue(dust >= 25, "a wreck at ($wx,$wy) must hold a dust bundle, got $dust")
        }
    }
}
