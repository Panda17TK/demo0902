package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.sim.RewardBundle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RewardApplyTest {
    @Test fun `the send-off lands on the player's components`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        with(gw.world) {
            val ammo = gw.player[Ammo]
            ammo.set("ammo9", 100)
            gw.player[Health].hp = 50f
            val blocksBefore = gw.player[Materials].blocks

            RewardApply.apply(gw.world, gw.player, RewardBundle(blocks = 2, ammoPct = 0.10f, med = 20f))

            assertEquals(blocksBefore + 2, gw.player[Materials].blocks)
            assertEquals(110, gw.player[Ammo].get("ammo9")) // +10%
            assertEquals(70f, gw.player[Health].hp)
        }
    }

    @Test fun `the ammo top-up rounds up so small pools still gain`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        with(gw.world) {
            gw.player[Ammo].set("ammoBeam", 6)
            RewardApply.apply(gw.world, gw.player, RewardBundle(ammoPct = 0.10f))
            assertEquals(7, gw.player[Ammo].get("ammoBeam")) // ceil(0.6) = 1
        }
    }

    @Test fun `healing never exceeds the maximum`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        with(gw.world) {
            val h = gw.player[Health]
            h.hp = h.hpMax - 5f
            RewardApply.apply(gw.world, gw.player, RewardBundle(med = 20f))
            assertEquals(h.hpMax, h.hp)
        }
    }
}
