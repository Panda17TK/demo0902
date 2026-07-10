package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** v2.144 大群衆: the sky teems thirtyfold, and every shoal is its own flock. */
class CrowdedSkyTest {
    private val dt = 1f / 60f

    @Test fun `the sky teems thirtyfold`() {
        for (seed in 1L..2L) {
            val gw = WorldFactory.create(InputState(), seed = seed)
            var wild = 0
            with(gw.world) { gw.world.family { all(Mob) }.forEach { if (it[Mob].def.lifeKind == LifeKind.WILDLIFE) wild++ } }
            assertTrue(wild in 4000..8000, "seed $seed: ~30x the old ~170 (got $wild)")
        }
    }

    @Test fun `the schools are many — separate flocks, not one great pool`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        val flocks = HashSet<Int>()
        with(gw.world) {
            gw.world.family { all(Mob) }.forEach { e ->
                val m = e[Mob]
                if (m.def.lifeKind == LifeKind.WILDLIFE && m.def.wildRole == WildRole.SCHOOL && m.schoolGroup != 0) flocks.add(m.schoolGroup)
            }
        }
        assertTrue(flocks.size >= 55, "many separate flocks fill the sky (got ${flocks.size})")
    }

    @Test fun `a stray keeps its own way — flocks of one species do not merge`() {
        val gw = WorldFactory.create(InputState(), seed = 5L)
        val def = GameConfig().enemies.getValue("star_sardine")
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        // flock A: thirty sardines in a ring; B: one stray AT the same spot with its own flock id.
        // Before v2.144 the stray would pool with them by kind and be swept along; now it swims on alone.
        repeat(30) { i ->
            val a = i * 0.2094f
            MobFactory.spawn(gw.world, def, px + 600f + cos(a) * 40f, py + sin(a) * 40f, schoolGroup = 900001)
        }
        val stray = MobFactory.spawn(gw.world, def, px + 600f, py, schoolGroup = 900002)
        repeat(300) { gw.world.update(dt) } // five seconds of open water
        var cx = 0f; var cy = 0f; var n = 0
        with(gw.world) {
            gw.world.family { all(Mob, Transform) }.forEach { e ->
                if (e[Mob].schoolGroup == 900001) { val t = e[Transform]; cx += t.x; cy += t.y; n++ }
            }
        }
        assertTrue(n >= 20, "the flock survives (got $n)")
        val (sx, sy) = with(gw.world) { stray[Transform].let { it.x to it.y } }
        assertTrue(hypot(sx - cx / n, sy - cy / n) > 200f, "the stray left the flock behind (${hypot(sx - cx / n, sy - cy / n)})")
    }
}
