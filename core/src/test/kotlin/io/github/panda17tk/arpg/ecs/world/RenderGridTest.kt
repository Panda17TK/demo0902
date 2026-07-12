package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.sim.WildLod
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.175 描画の倹約IV: the sim's spatial grid now serves the draw side too. */
class RenderGridTest {
    @Test fun `the mob grid is exposed and fills within one tick`() {
        val gw = WorldFactory.create(InputState(), seed = 5L, area = 1 to 1)
        gw.world.update(1f / 60f)
        var near = 0
        with(gw.world) {
            val t = gw.player[Transform]
            gw.mobGrid.forNearby(t.x, t.y, 2000f) { near++ }
        }
        assertTrue(near > 0, "the draw side sees the grid the sim maintains ($near nearby)")
    }

    @Test fun `the complete-grid radius covers any plausible camera extent`() {
        // the renderer trusts that everything on screen is in the grid EVERY tick — that holds
        // only while GRID_KEEP exceeds the camera's half-extent plus its cull margin.
        assertTrue(WildLod.GRID_KEEP >= 1200f, "GRID_KEEP shrank below a safe on-screen radius")
    }
}
