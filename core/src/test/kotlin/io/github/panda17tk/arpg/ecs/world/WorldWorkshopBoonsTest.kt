package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.WorkshopBoons
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.90 保守員の工房: the boons really reach the run — hull, breath, loot, and a faster mend. */
class WorldWorkshopBoonsTest {
    private fun clearMobs(gw: GameWorld) {
        val doomed = ArrayList<com.github.quillraven.fleks.Entity>()
        with(gw.world) { gw.world.family { all(Mob) }.forEach { doomed.add(it) } }
        for (e in doomed) gw.world -= e
    }

    @Test fun `hull, breath and loot land on the fresh run`() {
        val plain = WorldFactory.create(InputState(), seed = 3L)
        val boons = WorldFactory.create(
            InputState(), seed = 3L,
            boons = WorkshopBoons(hull = 30f, stamina = 30f, loot = 0.10f),
        )
        val plainHp = with(plain.world) { plain.player[Health].hpMax }
        val boonHp = with(boons.world) { boons.player[Health].hpMax }
        assertEquals(plainHp + 30f, boonHp, 1e-3f)
        val plainSta = with(plain.world) { plain.player[Stamina].max }
        val boonSta = with(boons.world) { boons.player[Stamina].max }
        assertEquals(plainSta + 30f, boonSta, 1e-3f)
        assertTrue(
            boons.worldState.spawnTweaks.bonusMaterialChance >= plain.worldState.spawnTweaks.bonusMaterialChance + 0.10f - 1e-4f,
            "拾集の目 raises the drop chance",
        )
    }

    @Test fun `the learned mend knits faster than the native one`() {
        val gw = WorldFactory.create(InputState(), seed = 3L, boons = WorkshopBoons(regenPerSec = 1f))
        with(gw.world) { gw.player[Health].hp = 50f }
        repeat((60 * 6.6f).toInt()) { clearMobs(gw); gw.world.update(1f / 60f) } // ≈4.1s of rest at 2/s
        val hp = with(gw.world) { gw.player[Health].hp }
        assertTrue(hp >= 57f, "2 hp/s after the quiet spell (got $hp)")
    }
}
