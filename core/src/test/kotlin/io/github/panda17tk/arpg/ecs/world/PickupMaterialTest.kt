package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mods
import io.github.panda17tk.arpg.ecs.components.Pickup
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PickupMaterialTest {
    private fun worldWithMaterialOnPlayer(kind: String): GameWorld {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        val (px, py) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
        gw.world.entity { it += Transform(x = px, y = py, prevX = px, prevY = py); it += Pickup(kind, 1) }
        gw.world.update(1f / 60f)
        return gw
    }

    @Test fun `a nature core raises the player's max HP`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        val maxBefore = with(gw.world) { gw.player[Health].hpMax }
        val (px, py) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
        gw.world.entity { it += Transform(x = px, y = py, prevX = px, prevY = py); it += Pickup("mat_nature", 1) }
        gw.world.update(1f / 60f)
        val maxAfter = with(gw.world) { gw.player[Health].hpMax }
        assertTrue(maxAfter > maxBefore, "max HP $maxBefore -> $maxAfter")
    }

    @Test fun `a magma core raises gun damage`() {
        val gw = worldWithMaterialOnPlayer("mat_magma")
        assertTrue(with(gw.world) { gw.player[Mods].gunMul } > 1f, "gunMul should exceed 1")
    }
}
