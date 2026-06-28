package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldEnemyTest {
    @Test fun `stage with enemy markers spawns mobs`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        val mobs = gw.world.family { all(Mob, Transform) }.numEntities
        assertTrue(mobs >= 1, "expected mobs spawned from stage markers, got $mobs")
    }

    @Test fun `a mob takes damage and dies when its health hits zero`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        val before = gw.world.family { all(Mob) }.numEntities
        // Set all mob health to -1 so MobDamageSystem reaps them next tick
        gw.world.family { all(Mob, Health) }.forEach { e ->
            with(gw.world) { e[Health].hp = -1f }
        }
        gw.world.update(1f / 60f)
        val after = gw.world.family { all(Mob) }.numEntities
        assertTrue(after < before, "dead mobs should be reaped: $before -> $after")
    }
}
