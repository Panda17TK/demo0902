package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.ecs.components.Pickup
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.math.Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.43 星屑: every kill sheds the trade currency, and walking over it banks it. */
class WorldDustTest {
    @Test fun `kill loot always includes a dust drop`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        Pickups.dropOnKill(gw.world, Rng(1L), 500f, 500f, boss = false)
        var dust = 0
        with(gw.world) {
            gw.world.family { all(Pickup, Transform) }.forEach { e ->
                if (e[Pickup].kind == "dust") dust += e[Pickup].amount
            }
        }
        assertTrue(dust in 2..5, "a normal kill sheds 2..5 dust, got $dust")
    }

    @Test fun `picking dust up banks it in Materials`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        val (px, py) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
        Pickups.spawn(gw.world, "dust", 7, px, py)
        gw.world.update(1f / 60f)
        assertEquals(7, with(gw.world) { gw.player[Materials].dust })
    }

    @Test fun `dust survives a world transition via PlayerCarry`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        with(gw.world) { gw.player[Materials].dust = 123 }
        val carry = PlayerCarry.of(gw.world, gw.player, wave = 2)
        val gw2 = WorldFactory.create(InputState(), seed = 4L, carry = carry)
        assertEquals(123, with(gw2.world) { gw2.player[Materials].dust })
    }
}
