package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.MobAction
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.88 ボス戦の山場化: the half-health rage latch and the boss-grade death ritual. */
class WorldBossPhaseTest {
    private fun spawnHeavy(gw: GameWorld, key: String, bounty: Int = 0): com.github.quillraven.fleks.Entity {
        val def = GameConfig().enemies.getValue(key)
        val (px, py) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
        val e = MobFactory.spawn(gw.world, def, px + 120f, py, 1, 0f, 0f, tribe = 0)
        if (bounty > 0) with(gw.world) { e[Mob].bountyDust = bounty }
        return e
    }

    @Test fun `a heavy rages past half health — a one-way latch on the enrage machinery`() {
        val gw = WorldFactory.create(InputState(), seed = 5L)
        val e = spawnHeavy(gw, "brute")
        with(gw.world) {
            val h = e[Health]
            h.hp = h.hpMax * 0.4f
        }
        gw.world.update(1f / 60f)
        with(gw.world) {
            assertTrue(e[Mob].phase2, "the latch flips at half health")
            assertTrue(e[MobAction].enrageT > 100f, "the rage does not cool")
            assertTrue(e[MobAction].enrageMul >= 1.35f, "swings quicken")
        }
        assertTrue(gw.fx.warnRings.isNotEmpty(), "the phase flip is marked in the world")
    }

    @Test fun `an ordinary hurt heavy above half health stays calm`() {
        val gw = WorldFactory.create(InputState(), seed = 5L)
        val e = spawnHeavy(gw, "brute")
        with(gw.world) { val h = e[Health]; h.hp = h.hpMax * 0.7f }
        gw.world.update(1f / 60f)
        with(gw.world) { assertTrue(!e[Mob].phase2, "no rage above half") }
    }

    @Test fun `a bounty kill earns the grand ritual — five bursts, a white-out, the long exhale`() {
        val gw = WorldFactory.create(InputState(), seed = 5L)
        val e = spawnHeavy(gw, "brute", bounty = 40)
        with(gw.world) { e[Health].hp = -1f }
        gw.world.update(1f / 60f)
        assertEquals(5, gw.fx.bursts.size, "a grand death chains five blasts")
        assertTrue(gw.fx.flashAlpha() > 0f, "the white-out crowns the kill")
        assertEquals(0.5f, gw.fx.slowmoT, 1e-3f, "the long exhale")
        assertTrue(gw.fx.particles.isNotEmpty(), "the gold shower rains")
    }
}
