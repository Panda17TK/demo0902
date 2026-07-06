package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.ecs.components.EBullet
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.world.GameWorld
import io.github.panda17tk.arpg.ecs.world.WorldFactory
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.97 難易度: the three run modes and their multipliers — pure rules + one live wound. */
class DifficultyTest {
    @Test fun `the three modes carry their promised numbers`() {
        assertEquals(0.7f, Difficulty.CALM.dmgTakenMul)
        assertEquals(1.5f, Difficulty.CALM.regenMul)
        assertEquals(1f, Difficulty.NORMAL.dmgTakenMul)
        assertEquals(1.3f, Difficulty.OVERLOAD.quotaMul)
        assertEquals(1, Difficulty.OVERLOAD.dustBonus)
        assertEquals(0, Difficulty.CALM.dustBonus)
    }

    @Test fun `the chip cycles and unknown names fall back to normal`() {
        assertEquals(Difficulty.NORMAL, Difficulty.CALM.next())
        assertEquals(Difficulty.OVERLOAD, Difficulty.NORMAL.next())
        assertEquals(Difficulty.CALM, Difficulty.OVERLOAD.next())
        assertEquals(Difficulty.NORMAL, Difficulty.byName("no_such_mode"))
        assertEquals(Difficulty.CALM, Difficulty.byName("CALM"))
    }

    private fun woundAt(difficulty: Difficulty): Float {
        val gw = WorldFactory.create(InputState(), seed = 3L, difficulty = difficulty)
        // an empty sky — no stray drifter fire corrupting the measurement
        val doomed = ArrayList<com.github.quillraven.fleks.Entity>()
        with(gw.world) { gw.world.family { all(Mob) }.forEach { doomed.add(it) } }
        for (e in doomed) gw.world -= e
        val (px, py) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
        gw.world.entity {
            it += Transform(x = px, y = py, prevX = px, prevY = py)
            it += EBullet(0f, 0f, 2f, 20f)
        }
        val before = hp(gw)
        repeat(3) { gw.world.update(1f / 60f) }
        return before - hp(gw)
    }

    private fun hp(gw: GameWorld): Float = with(gw.world) { gw.player[Health].hp }

    @Test fun `安定運転 really softens the wound`() {
        // the starter armor already shaves a little — assert the RATIO the mode promises
        val normal = woundAt(Difficulty.NORMAL)
        val calm = woundAt(Difficulty.CALM)
        assertTrue(normal > 15f, "the test wound lands (got $normal)")
        assertEquals(0.7f, calm / normal, 0.02f, "安定運転 takes 0.7x of the same hit")
    }
}
