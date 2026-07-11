package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.world.GameWorld
import io.github.panda17tk.arpg.ecs.world.WorldFactory
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.hypot

/** v2.164 軽い海: the far ocean ticks on a stagger — same paths, a fraction of the cost. */
class WildLodTest {
    @Test fun `the damage grid stays complete out to the railgun's reach`() {
        // v2.169 診断修正: RAIL_RANGE (1600) > MID (1500) let rail slugs pass through fish whose
        // grid insertion was staggered — GRID_KEEP must cover every weapon's reach.
        assertTrue(WildLod.GRID_KEEP > io.github.panda17tk.arpg.ecs.systems.FireSystem.RAIL_RANGE)
        assertTrue(WildLod.GRID_KEEP > WildLod.MID)
    }

    private val dt = 1f / 60f

    @Test fun `the stride ladder and its stagger cover every fish exactly once per cycle`() {
        assertEquals(1, WildLod.stride(0f))
        assertEquals(1, WildLod.stride(WildLod.MID2))
        assertEquals(WildLod.MID_STRIDE, WildLod.stride(WildLod.MID2 + 1f))
        assertEquals(WildLod.FAR_STRIDE, WildLod.stride(WildLod.FAR2 + 1f))
        for (id in 0..9) {
            val due = (0 until WildLod.MID_STRIDE).count { tick -> WildLod.due(WildLod.MID_STRIDE, tick, id) }
            assertEquals(1, due, "one turn per cycle for id=$id")
        }
        assertTrue(WildLod.due(1, 3, 7), "near wildlife ticks every frame")
    }

    @Test fun `the LOD ocean is deterministic and the far schools still swim`() {
        fun run(seed: Long): Pair<GameWorld, Map<Int, Pair<Float, Float>>> {
            val gw = WorldFactory.create(InputState(), seed = seed)
            repeat(180) { gw.world.update(dt) }
            val out = HashMap<Int, Pair<Float, Float>>()
            with(gw.world) {
                gw.world.family { all(Mob, Transform) }.forEach { e ->
                    if (e[Mob].def.wildRole == WildRole.SCHOOL) out[e.id] = e[Transform].x to e[Transform].y
                }
            }
            return gw to out
        }
        val (_, a) = run(9L)
        val (_, b) = run(9L)
        assertEquals(a, b, "same seed, same ocean — the stagger keys on the sim, not the clock")
    }

    @Test fun `a school beyond the far ring keeps swimming, not frozen`() {
        val gw = WorldFactory.create(InputState(), seed = 9L)
        val (px, py) = with(gw.world) { gw.world.family { all(PlayerTag, Transform) }.first()[Transform].let { it.x to it.y } }
        val before = HashMap<Int, Pair<Float, Float>>()
        with(gw.world) {
            gw.world.family { all(Mob, Transform) }.forEach { e ->
                val t = e[Transform]
                if (e[Mob].def.wildRole == WildRole.SCHOOL && hypot(t.x - px, t.y - py) > WildLod.FAR) {
                    before[e.id] = t.x to t.y
                }
            }
        }
        assertTrue(before.size > 50, "the far ocean is populated (got ${before.size})")
        repeat(120) { gw.world.update(dt) }
        var moved = 0
        with(gw.world) {
            gw.world.family { all(Mob, Transform) }.forEach { e ->
                val start = before[e.id] ?: return@forEach
                val t = e[Transform]
                if (hypot(t.x - start.first, t.y - start.second) > 10f) moved++
            }
        }
        assertTrue(moved > before.size / 2, "far fish still swim ($moved/${before.size} moved)")
    }
}
