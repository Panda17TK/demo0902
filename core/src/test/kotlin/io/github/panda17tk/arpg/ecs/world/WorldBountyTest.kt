package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Pickup
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.sim.WaveEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.45: bounty heads pay their dust pile on death; kills tally toward the planet's request. */
class WorldBountyTest {
    private fun midbossDef() = GameConfig().enemies.values.first { it.tier == "midboss" && it.biome == null }
    private fun normalDef() = GameConfig().enemies.values.first { it.tier == "normal" && it.biome == null }

    @Test fun `a fallen bounty head bursts into its exact dust pile`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        val e = MobFactory.spawn(gw.world, midbossDef(), 500f, 500f)
        with(gw.world) { e[Mob].bountyDust = 77; e[Health].hp = -1f }
        gw.world.update(1f / 60f)
        var exactPile = 0
        with(gw.world) {
            gw.world.family { all(Pickup, Transform) }.forEach { p ->
                if (p[Pickup].kind == "dust" && p[Pickup].amount == 77) exactPile++
            }
        }
        assertEquals(1, exactPile, "the 77-dust bounty pile must drop once")
        assertTrue(gw.waveState.announce?.contains("賞金首") == true, "the HUD is told about the payout")
    }

    @Test fun `kills tally toward the planet's request`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        val before = gw.worldState.questKills
        val n = MobFactory.spawn(gw.world, normalDef(), 500f, 500f)
        val m = MobFactory.spawn(gw.world, midbossDef(), 600f, 600f)
        with(gw.world) { n[Health].hp = -1f; m[Health].hp = -1f }
        gw.world.update(1f / 60f)
        assertEquals(before + 2, gw.worldState.questKills, "both kills count")
        assertEquals(1, gw.worldState.questElites, "only the midboss is an elite")
    }

    @Test fun `a storm wave doubles the dust a kill sheds`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        gw.waveState.event = WaveEvent.STORM
        val n = MobFactory.spawn(gw.world, normalDef(), 500f, 500f)
        with(gw.world) { n[Health].hp = -1f }
        gw.world.update(1f / 60f)
        var dust = 0
        with(gw.world) {
            gw.world.family { all(Pickup, Transform) }.forEach { p ->
                if (p[Pickup].kind == "dust") dust += p[Pickup].amount
            }
        }
        assertTrue(dust in 4..10 && dust % 2 == 0, "storm dust is (2..5)×2, got $dust")
    }
}
