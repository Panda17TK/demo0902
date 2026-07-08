package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.sim.Tuning
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.hypot

/** v2.136 泳ぎ続ける海: fish never hang dead in the void, and rocks turn them instead of trapping them. */
class WorldSwimTest {
    private val dt = 1f / 60f

    @Test fun `space fish keep swimming even with no one around`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        val start = HashMap<Int, Pair<Float, Float>>()
        with(gw.world) {
            gw.world.family { all(Mob, Transform) }.forEach { e ->
                val m = e[Mob]
                if (m.def.swims && m.def.wildRole != WildRole.SCHOOL) {
                    val t = e[Transform]; start[e.id] = t.x to t.y
                }
            }
        }
        assertTrue(start.size >= 5, "the sky hosts loose fish (got ${start.size})")
        repeat(300) { gw.world.update(dt) } // five seconds, player far away at the pad
        var moved = 0
        with(gw.world) {
            gw.world.family { all(Mob, Transform) }.forEach { e ->
                val s = start[e.id] ?: return@forEach
                val t = e[Transform]
                if (hypot(t.x - s.first, t.y - s.second) > 30f) moved++
            }
        }
        assertTrue(moved >= start.size * 6 / 10, "most fish swim on ($moved/${start.size} moved > 30px)")
    }

    @Test fun `a school fish bounces off the rock instead of pinning against it`() {
        val gw = WorldFactory.create(InputState(), seed = 2L)
        val def = GameConfig().enemies.getValue("star_sardine")
        // drop one sardine right at the western border wall, swimming straight into it
        val y0 = gw.map.height * Tuning.TILE / 2f
        val fish = MobFactory.spawn(gw.world, def, Tuning.TILE * 1.5f, y0)
        with(gw.world) { fish[Facing].x = -1f; fish[Facing].y = 0f }
        val (x0, yy0) = with(gw.world) { fish[Transform].let { it.x to it.y } }
        repeat(120) { gw.world.update(dt) }
        val (x1, y1) = with(gw.world) { fish[Transform].let { it.x to it.y } }
        assertTrue(hypot(x1 - x0, y1 - yy0) > 20f, "the fish swims away from the wall (moved ${hypot(x1 - x0, y1 - yy0)})")
    }

    @Test fun `the island whale drifts instead of hanging still`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        val def = GameConfig().enemies.getValue("isle_whale")
        assertTrue(def.swims, "the whale is a space fish")
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        val whale = MobFactory.spawn(gw.world, def, px + 600f, py)
        repeat(300) { gw.world.update(dt) }
        val moved = with(gw.world) { whale[Transform].let { hypot(it.x - (px + 600f), it.y - py) } }
        assertTrue(moved > 15f, "the island drifts (moved $moved)")
    }
}
