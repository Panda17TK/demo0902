package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Mods
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldUpgradeTest {
    @Test fun `player has Mods defaulting to identity`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        with(gw.world) {
            val m = gw.player[Mods]
            assertEquals(1f, m.gunMul, 1e-4f)
            assertEquals(0f, m.healOnKill, 1e-4f)
        }
    }

    @Test fun `healOnKill heals the player when a mob is reaped`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        with(gw.world) {
            gw.player[Mods].healOnKill = 5f
            gw.player[Health].hp = 10f
        }
        gw.world.family { all(Mob, Health) }.forEach { e -> with(gw.world) { e[Health].hp = -1f } }
        gw.world.update(1f / 60f)
        with(gw.world) { assertTrue(gw.player[Health].hp > 10f, "player should heal on kill") }
    }

    @Test fun `healOnKill does not overheal past hpMax`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        with(gw.world) {
            gw.player[Mods].healOnKill = 999f
            gw.player[Health].hp = gw.player[Health].hpMax - 1f
        }
        gw.world.family { all(Mob, Health) }.forEach { e -> with(gw.world) { e[Health].hp = -1f } }
        gw.world.update(1f / 60f)
        with(gw.world) { assertEquals(gw.player[Health].hpMax, gw.player[Health].hp, 1e-3f) }
    }
}
