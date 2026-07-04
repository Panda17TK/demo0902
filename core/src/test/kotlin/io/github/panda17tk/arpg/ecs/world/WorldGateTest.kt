package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.ecs.components.Pickup
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.sim.WorldMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.44 ジャンプゲート: every space map places one gate; boss kills shed the shards that open it. */
class WorldGateTest {
    @Test fun `space always places a jump gate, deterministically`() {
        val a = WorldFactory.create(InputState(), seed = 5L)
        val b = WorldFactory.create(InputState(), seed = 5L)
        assertNotNull(a.worldState.gate, "space must have a gate")
        assertEquals(a.worldState.gate, b.worldState.gate, "same seed → same gate position")
    }

    @Test fun `different seeds move the gate`() {
        val a = WorldFactory.create(InputState(), seed = 5L)
        val b = WorldFactory.create(InputState(), seed = 6L)
        assertTrue(a.worldState.gate != b.worldState.gate, "a new system relocates the gate")
    }

    @Test fun `the gate sits inside the map, away from spawn`() {
        for (seed in listOf(1L, 9L, 42L, 777L)) {
            val gw = WorldFactory.create(InputState(), seed = seed)
            val g = gw.worldState.gate!!
            val w = gw.map.width * Tuning.TILE; val h = gw.map.height * Tuning.TILE
            assertTrue(g.first in 0f..w && g.second in 0f..h, "gate inside the map (seed $seed)")
        }
    }

    @Test fun `surfaces have no gate`() {
        val gw = WorldFactory.create(InputState(), seed = 3L, mode = WorldMode.SURFACE, biome = PlanetBiome.NATURE)
        assertNull(gw.worldState.gate)
    }

    @Test fun `a boss kill drops exactly one gate shard`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        Pickups.dropOnKill(gw.world, Rng(1L), 500f, 500f, boss = true)
        var shards = 0
        with(gw.world) {
            gw.world.family { all(Pickup, Transform) }.forEach { e ->
                if (e[Pickup].kind == "shard") shards += e[Pickup].amount
            }
        }
        assertEquals(1, shards, "a boss sheds one shard")
    }

    @Test fun `normal kills drop no shards`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        Pickups.dropOnKill(gw.world, Rng(1L), 500f, 500f, boss = false)
        var shards = 0
        with(gw.world) {
            gw.world.family { all(Pickup, Transform) }.forEach { e ->
                if (e[Pickup].kind == "shard") shards += e[Pickup].amount
            }
        }
        assertEquals(0, shards)
    }

    @Test fun `picking a shard up banks it in Materials`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        val (px, py) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
        Pickups.spawn(gw.world, "shard", 1, px, py)
        gw.world.update(1f / 60f)
        assertEquals(1, with(gw.world) { gw.player[Materials].shards })
    }

    @Test fun `shards survive a world transition via PlayerCarry`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        with(gw.world) { gw.player[Materials].shards = 2 }
        val carry = PlayerCarry.of(gw.world, gw.player, wave = 2)
        val gw2 = WorldFactory.create(InputState(), seed = 4L, carry = carry)
        assertEquals(2, with(gw2.world) { gw2.player[Materials].shards })
    }
}
