package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.113 図鑑: the sim counts kills by kind, deterministically, on the world's GameOver. */
class WorldBestiaryTest {
    @Test fun `the reaper files each kill under its kind`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        val z = GameConfig().enemies.getValue("zombie").copy(attacks = emptyList())
        val a = MobFactory.spawn(gw.world, z, px + 200f, py)
        val b = MobFactory.spawn(gw.world, z, px + 220f, py)
        with(gw.world) { a[Health].hp = 0f; b[Health].hp = 0f }
        repeat(3) { gw.world.update(1f / 60f) }
        assertEquals(2, gw.gameOver.killsByKind["zombie"], "two zombies, two marks under one kind")
        assertTrue(gw.gameOver.kills >= 2)
    }
}
