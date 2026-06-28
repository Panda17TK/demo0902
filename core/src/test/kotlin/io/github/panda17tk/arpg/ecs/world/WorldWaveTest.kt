package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldWaveTest {
    @Test fun `wave spawner produces enemies over time`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        // Remove the initial marker mobs so this isolates wave spawning.
        gw.world.family { all(Mob, Health) }.forEach { e -> with(gw.world) { e[Health].hp = -1f } }
        gw.world.update(1f / 60f) // reap them
        repeat(400) { gw.world.update(1f / 60f) } // ~6.6s of the active wave
        val spawned = gw.world.family { all(Mob) }.numEntities
        assertTrue(spawned > 0, "the wave spawner should produce enemies, got $spawned")
    }
}
