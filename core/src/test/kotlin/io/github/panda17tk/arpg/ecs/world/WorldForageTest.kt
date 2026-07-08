package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.ecs.components.Pickup
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.128 回収の手触り: the reach doubled, and now and then the keeper snaps at the morsel. */
class WorldForageTest {
    private val dt = 1f / 60f

    @Test fun `a pickup 30px away is collected — inside the doubled reach, outside the old one`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        val dustBefore = with(gw.world) { gw.player[Materials].dust }
        gw.world.entity { it += Transform(x = px + 30f, y = py, prevX = px + 30f, prevY = py); it += Pickup("dust", 3) }
        gw.world.update(dt)
        assertEquals(dustBefore + 3, with(gw.world) { gw.player[Materials].dust }, "30px sits inside the new 36px reach")

        val far = WorldFactory.create(InputState(), seed = 1L)
        val (qx, qy) = with(far.world) { far.player[Transform].let { it.x to it.y } }
        val farBefore = with(far.world) { far.player[Materials].dust }
        far.world.entity { it += Transform(x = qx + 60f, y = qy, prevX = qx + 60f, prevY = qy); it += Pickup("dust", 3) }
        far.world.update(dt)
        assertEquals(farBefore, with(far.world) { far.player[Materials].dust }, "60px stays out of reach")
    }

    @Test fun `some morsels trigger the snap, deterministically by position, without touching the rng`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        // find offsets whose position hash does / does not select the snap
        fun hashes(ox: Float) = (((px + ox).toInt() * 31 + py.toInt() * 17) and 3) == 0
        val snapOx = (10..34).map { it.toFloat() }.first { hashes(it) }
        val quietOx = (10..34).map { it.toFloat() }.first { !hashes(it) }

        gw.world.entity { it += Transform(x = px + quietOx, y = py, prevX = px + quietOx, prevY = py); it += Pickup("dust", 1) }
        gw.world.update(dt)
        assertEquals(0f, gw.fx.chompT, "a quiet morsel passes without the snap")

        gw.world.entity { it += Transform(x = px + snapOx, y = py, prevX = px + snapOx, prevY = py); it += Pickup("dust", 1) }
        gw.world.update(dt)
        assertTrue(gw.fx.chompT > 0f, "the hashed morsel triggers the snap")
        assertTrue(gw.fx.chompDx > 0.9f, "the snap points at the morsel (got ${gw.fx.chompDx}, ${gw.fx.chompDy})")
    }
}
