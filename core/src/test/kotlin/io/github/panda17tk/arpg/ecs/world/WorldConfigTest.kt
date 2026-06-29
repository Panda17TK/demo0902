package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.PlayerConfig
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldConfigTest {
    // Movement is acceleration-based now, so baseSpeed sets the *speed cap*: after a short ramp the
    // player with a higher baseSpeed reaches a higher steady velocity. (Spawn area is wall-clear, so
    // the warm-up doesn't collide.)
    @Test fun `higher baseSpeed in config raises the player's movement speed`() {
        fun speedAfterWarmup(baseSpeed: Float): Float {
            val input = InputState().apply { right = true }
            val cfg = GameConfig(player = PlayerConfig(baseSpeed = baseSpeed))
            val gw = WorldFactory.create(input, cfg, seed = 1L)
            repeat(30) { gw.world.update(1f / 60f) }
            return with(gw.world) { gw.player[Velocity].driftX }
        }
        val slow = speedAfterWarmup(110f)
        val fast = speedAfterWarmup(330f)
        assertTrue(fast > slow * 1.5f, "higher baseSpeed → higher speed: slow=$slow fast=$fast")
    }
}
