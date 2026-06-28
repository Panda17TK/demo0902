package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.Bullet
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldCombatTest {
    @Test fun `firing the pistol spawns a bullet and consumes a magazine round`() {
        val input = InputState().apply { fire = true }
        val gw = WorldFactory.create(input, seed = 1L)
        val magBefore = with(gw.world) { gw.player[Arsenal].current.mag }
        gw.world.update(1f / 60f)
        val magAfter = with(gw.world) { gw.player[Arsenal].current.mag }
        assertEquals(magBefore - 1, magAfter)
        val bullets = gw.world.family { all(Bullet) }.numEntities
        assertTrue(bullets >= 1, "expected a bullet entity, got $bullets")
    }

    @Test fun `reload refills the magazine from reserves`() {
        val input = InputState()
        val gw = WorldFactory.create(input, seed = 1L)
        // drain a few rounds
        input.fire = true
        repeat(3) { gw.world.update(0.3f) } // fireRate 0.22 -> 3 shots
        input.fire = false
        val magLow = with(gw.world) { gw.player[Arsenal].current.mag }
        input.reload = true
        gw.world.update(1f / 60f)
        input.reload = false
        val magFull = with(gw.world) { gw.player[Arsenal].current.mag }
        assertTrue(magFull > magLow, "reload should refill: $magLow -> $magFull")
    }
}
