package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.PlayerConfig
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldConfigTest {
    @Test fun `higher baseSpeed in config makes the player move farther`() {
        fun distAfterOneTick(baseSpeed: Float): Float {
            val input = InputState().apply { right = true }
            val cfg = GameConfig(player = PlayerConfig(baseSpeed = baseSpeed))
            val gw = WorldFactory.create(input, cfg, seed = 1L)
            val x0 = with(gw.world) { gw.player[Transform].x }
            gw.world.update(1f / 60f)
            val x1 = with(gw.world) { gw.player[Transform].x }
            return x1 - x0
        }
        val slow = distAfterOneTick(110f)
        val fast = distAfterOneTick(330f)
        assertTrue(fast > slow * 2.5f, "3x baseSpeed should move ~3x farther: slow=$slow fast=$fast")
    }
}
