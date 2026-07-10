package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.hypot

/** v2.143 仕上げ: the coverage gaps the deep audit listed — pickup reach, whale coupling, tunable windup. */
class PolishGapsTest {
    private val dt = 1f / 60f

    @Test fun `the doubled pickup reach collects at 30px and not at 60px`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        Pickups.spawn(gw.world, "dust", 5, px + 30f, py) // 18 < 30 < 36 — v2.128's widened reach
        gw.world.update(dt)
        assertEquals(5, with(gw.world) { gw.player[Materials].dust }, "30px is within the doubled reach")
        Pickups.spawn(gw.world, "dust", 7, px + 60f, py) // beyond the reach (and the magnet is off)
        gw.world.update(dt)
        assertEquals(5, with(gw.world) { gw.player[Materials].dust }, "60px stays on the floor")
    }

    @Test fun `a world-spawned whale always brings its retinue`() {
        var found = false
        for (seed in 1L..12L) {
            val gw = WorldFactory.create(InputState(), seed = seed)
            var wx = 0f; var wy = 0f; var whale = false; var pilots = 0
            with(gw.world) {
                gw.world.family { all(Mob, Transform) }.forEach { e ->
                    if (e[Mob].def.id == "isle_whale") { whale = true; wx = e[Transform].x; wy = e[Transform].y }
                }
                if (whale) {
                    gw.world.family { all(Mob, Transform) }.forEach { e ->
                        val t = e[Transform]
                        if (e[Mob].def.id == "pilot_minnow" && hypot(t.x - wx, t.y - wy) < 400f) pilots++
                    }
                }
            }
            if (whale) {
                found = true
                assertTrue(pilots >= 20, "seed $seed: the whale trails its retinue (got $pilots pilots)")
                break
            }
        }
        assertTrue(found, "no whale sky in seeds 1..12")
    }

    @Test fun `the bite windup knob reaches the hunt`() {
        val cfg = GameConfig().also { it.wild.biteWindup = 0.1f } // 調整モード shortens the telegraph
        val gw = WorldFactory.create(InputState(), config = cfg, seed = 3L)
        val apex = cfg.enemies.getValue("forest_apex")
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        MobFactory.spawn(gw.world, apex, px + 20f, py)
        val hpBefore = with(gw.world) { gw.player[Health].hp }
        repeat(20) { gw.world.update(dt) } // 0.1s windup ≈ 6 ticks — well inside 20
        assertTrue(with(gw.world) { gw.player[Health].hp } < hpBefore, "the shortened lunge lands early")
    }
}
