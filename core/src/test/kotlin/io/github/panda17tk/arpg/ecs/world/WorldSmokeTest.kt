package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldSmokeTest {
    @Test fun `player moves right when right is held`() {
        val input = InputState().apply { right = true }
        val gw = WorldFactory.create(input)
        val startX = with(gw.world) { gw.player[Transform].x }
        gw.world.update(0.1f)
        val endX = with(gw.world) { gw.player[Transform].x }
        assertTrue(endX > startX, "expected player.x to increase, was $startX -> $endX")
    }

    @Test fun `snapshot records previous position before movement`() {
        val input = InputState().apply { right = true }
        val gw = WorldFactory.create(input)
        val startX = with(gw.world) { gw.player[Transform].x }
        gw.world.update(0.1f)
        with(gw.world) {
            // prev should equal the position at the start of the tick (before this step's move)
            assertEquals(startX, gw.player[Transform].prevX, 1e-4f)
            assertTrue(gw.player[Transform].x > gw.player[Transform].prevX)
        }
    }
}
