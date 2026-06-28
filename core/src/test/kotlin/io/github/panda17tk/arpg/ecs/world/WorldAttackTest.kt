package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldAttackTest {
    @Test fun `enemies damage the player when adjacent`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        // Move the player on top of the first mob so it has LOS + range.
        var done = false
        gw.world.family { all(Mob, Transform) }.forEach { e ->
            if (done) return@forEach
            val mx = with(gw.world) { e[Transform].x }
            val my = with(gw.world) { e[Transform].y }
            gw.world.family { all(PlayerTag, Transform) }.forEach { p -> with(gw.world) { p[Transform].x = mx + 20f; p[Transform].y = my } }
            done = true
        }
        val hp0 = playerHp(gw)
        repeat(240) { gw.world.update(1f / 60f) } // ~4s of combat
        val hp1 = playerHp(gw)
        assertTrue(hp1 < hp0, "player should take damage from adjacent enemies: $hp0 -> $hp1")
    }

    private fun playerHp(gw: GameWorld): Float {
        var hp = 0f
        gw.world.family { all(PlayerTag, Health) }.forEach { e -> with(gw.world) { hp = e[Health].hp } }
        return hp
    }
}
