package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.92: the sim's feat counters — rogues, raging heavies, grand rituals. */
class WorldFeatCountersTest {
    @Test fun `a fallen rogue drifter ticks the rogue counter`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        var killed = 0
        with(gw.world) {
            gw.world.family { all(Mob, Health) }.forEach { e ->
                if (e[Mob].def.tier == "rogue") { e[Health].hp = -1f; killed++ }
            }
        }
        assertTrue(killed > 0, "the system carries its two rogues")
        gw.world.update(1f / 60f)
        assertEquals(killed, gw.gameOver.rogueKills)
    }

    @Test fun `a heavy felled mid-rage ticks the rage counter`() {
        val gw = WorldFactory.create(InputState(), seed = 5L)
        val def = GameConfig().enemies.getValue("brute")
        val (px, py) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
        val e = MobFactory.spawn(gw.world, def, px + 120f, py, 1, 0f, 0f, tribe = 0)
        with(gw.world) { e[Health].hp = e[Health].hpMax * 0.4f }
        gw.world.update(1f / 60f) // the rage latch flips
        with(gw.world) { e[Health].hp = -1f }
        gw.world.update(1f / 60f)
        assertEquals(1, gw.gameOver.rageKills)
    }

    @Test fun `the grand ritual is counted for bosses and bounty heads`() {
        val gw = WorldFactory.create(InputState(), seed = 5L)
        val def = GameConfig().enemies.getValue("brute")
        val (px, py) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
        val e = MobFactory.spawn(gw.world, def, px + 120f, py, 1, 0f, 0f, tribe = 0)
        with(gw.world) { e[Mob].bountyDust = 30; e[Health].hp = -1f }
        gw.world.update(1f / 60f)
        assertEquals(1, gw.gameOver.grandKills)
    }
}
