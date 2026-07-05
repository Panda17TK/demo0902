package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.sim.Tribes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.83: standing still mends the keeper (+1/s after a quiet spell); action stops it.
 *  Also: the two rogue drifters ride the ROGUE banner, hostile to every tribe. */
class WorldRestRegenTest {
    /** The rest tests want an empty sky — no drifter potshots corrupting the measurement. */
    private fun clearMobs(gw: GameWorld) {
        val doomed = ArrayList<com.github.quillraven.fleks.Entity>()
        with(gw.world) { gw.world.family { all(Mob) }.forEach { doomed.add(it) } }
        for (e in doomed) gw.world -= e
    }

    @Test fun `standing still knits one hp per second after the quiet spell`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        with(gw.world) { gw.player[Health].hp = 50f }
        repeat((60 * 6.6f).toInt()) { clearMobs(gw); gw.world.update(1f / 60f) } // 6.6s − 2.5s delay ≈ 4 ticks
        val hp = with(gw.world) { gw.player[Health].hp }
        assertTrue(hp >= 53f, "resting should have mended a few points (got $hp)")
    }

    @Test fun `holding the trigger keeps the wounds open`() {
        val input = InputState().apply { fire = true }
        val gw = WorldFactory.create(input, seed = 3L)
        with(gw.world) { gw.player[Health].hp = 50f }
        repeat(60 * 5) { clearMobs(gw); gw.world.update(1f / 60f) }
        val hp = with(gw.world) { gw.player[Health].hp }
        assertTrue(hp <= 50.01f, "firing must suppress the mending (got $hp)")
    }

    @Test fun `two rogue drifters coast the system under the bannerless tribe`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        var rogues = 0
        with(gw.world) {
            gw.world.family { all(Mob) }.forEach { e ->
                if (e[Mob].def.tier == "rogue") { rogues++; assertEquals(Tribes.ROGUE, e[Mob].tribe) }
            }
        }
        assertEquals(2, rogues, "exactly two rogue drifters per system")
        // and the banner is at war with everyone, both ways
        val tribes = Tribes.build(4, 1000f, 1000f, 0.5f, io.github.panda17tk.arpg.math.Rng(1L))
        for (t in 0 until tribes.count) {
            assertTrue(tribes.areHostile(Tribes.ROGUE, t) && tribes.areHostile(t, Tribes.ROGUE), "rogue must fight tribe $t")
        }
        assertTrue(!tribes.areHostile(Tribes.ROGUE, Tribes.ROGUE), "a rogue is not at war with itself")
    }
}
