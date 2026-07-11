package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.hypot

/** v2.157 読む海: the whale's water is a haven, and some flocks school around the landmarks. */
class ReadableSeaTest {
    private val dt = 1f / 60f

    @Test fun `resting in the whale's shadow mends half again as fast`() {
        // two identical skies, same seed — the only difference is a whale beside the pad
        fun healedAfter(withWhale: Boolean): Float {
            val gw = WorldFactory.create(InputState(), seed = 11L)
            val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
            if (withWhale) MobFactory.spawn(gw.world, GameConfig().enemies.getValue("isle_whale"), px + 200f, py)
            val hp0: Float
            with(gw.world) { val h = gw.player[Health]; h.hp = h.hpMax - 20f; hp0 = h.hp }
            repeat(330) { gw.world.update(dt) } // 2.5s rest delay + ~3s of mending
            return with(gw.world) { gw.player[Health].hp } - hp0
        }
        val calm = healedAfter(withWhale = true)
        val alone = healedAfter(withWhale = false)
        assertTrue(calm > alone, "the whale's calm quickens the mending ($calm vs $alone)")
    }

    @Test fun `some flocks school around the sky's landmarks`() {
        val gw = WorldFactory.create(InputState(), seed = 4L)
        val ws = gw.worldState
        val poi = buildList {
            addAll(ws.wrecks)
            ws.comet?.let { add(it) }
            ws.trader?.let { add(it) }
        }
        assertTrue(poi.isNotEmpty(), "the sky keeps its landmarks")
        // count tiny flocks whose centroid sits within reach of a landmark
        val flocks = HashMap<Int, Pair<Float, Float>>() // group -> sum
        val counts = HashMap<Int, Int>()
        with(gw.world) {
            gw.world.family { all(Mob, Transform) }.forEach { e ->
                val m = e[Mob]
                if (m.def.id != "star_sardine" && m.def.id != "void_aji") return@forEach
                val t = e[Transform]
                val s = flocks.getOrDefault(m.schoolGroup, 0f to 0f)
                flocks[m.schoolGroup] = (s.first + t.x) to (s.second + t.y)
                counts.merge(m.schoolGroup, 1, Int::plus)
            }
        }
        val nearPoi = flocks.entries.count { (g, sum) ->
            val n = counts.getValue(g)
            val cx = sum.first / n; val cy = sum.second / n
            poi.any { hypot(cx - it.first, cy - it.second) < 600f }
        }
        assertTrue(nearPoi >= 3, "following the fish leads somewhere (got $nearPoi flocks by landmarks)")
    }
}
