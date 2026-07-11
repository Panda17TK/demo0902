package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** v2.149 倹約第3弾: the per-flock predator prefilter must not dull the flee — teeth still scatter. */
class FlockFleeTest {
    private val dt = 1f / 60f

    @Test fun `a shark inside the flock still scatters it after the per-flock prefilter`() {
        val gw = WorldFactory.create(InputState(), seed = 5L)
        val defs = GameConfig().enemies
        val sardine = defs.getValue("star_sardine")
        val shark = defs.getValue("void_shark")
        val (px0, py0) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        val cx = px0 + 500f; val cy = py0
        repeat(24) { i ->
            val a = i * 0.2618f
            MobFactory.spawn(gw.world, sardine, cx + cos(a) * 30f, cy + sin(a) * 30f, schoolGroup = 900201)
        }
        MobFactory.spawn(gw.world, shark, cx, cy)
        repeat(120) { gw.world.update(dt) } // two seconds of terror
        var sum = 0.0; var n = 0
        with(gw.world) {
            gw.world.family { all(Mob, Transform) }.forEach { e ->
                if (e[Mob].schoolGroup != 900201) return@forEach
                val t = e[Transform]; sum += hypot(t.x - cx, t.y - cy).toDouble(); n++
            }
        }
        assertTrue(n >= 15, "the flock survives the two seconds (got $n)")
        assertTrue(sum / n > 100.0, "the flock bolted from the shark (mean ${sum / n})")
    }
}
