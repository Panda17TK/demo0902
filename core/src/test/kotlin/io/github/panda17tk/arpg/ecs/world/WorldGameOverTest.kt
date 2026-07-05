package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldGameOverTest {
    @Test fun `game over flag is set when player hp is depleted`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        assertFalse(gw.gameOver.isOver)
        with(gw.world) { gw.player[Health].hp = -1f }
        gw.world.update(1f / 60f)
        assertTrue(gw.gameOver.isOver)
    }

    @Test fun `mob death stages a corpse, then bursts into gib particles`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        gw.world.family { all(Mob, Health) }.forEach { e -> with(gw.world) { e[Health].hp = -1f } }
        gw.world.update(1f / 60f)
        // v2.85 段階的な死: the body squashes out first; the gibs burst a tenth of a second later.
        assertTrue(gw.fx.corpses.isNotEmpty(), "death should stage a corpse")
        gw.fx.update(0.15f) // render-side time — the delayed burst fires
        assertTrue(gw.fx.particles.isNotEmpty(), "the staged burst should spawn gib particles")
    }
}
