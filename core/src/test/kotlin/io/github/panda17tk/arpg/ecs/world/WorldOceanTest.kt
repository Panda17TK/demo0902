package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.render.CreatureLook
import io.github.panda17tk.arpg.sim.Predation
import io.github.panda17tk.arpg.sim.Tuning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** v2.135 満ちる海: the ocean fills the whole sky, the tyrant hunts the keeper, the whale trails its retinue. */
class WorldOceanTest {
    private val dt = 1f / 60f

    @Test fun `the ocean fills every sector of the sky`() {
        for (seed in 1L..2L) {
            val gw = WorldFactory.create(InputState(), seed = seed)
            val ww = gw.map.width * Tuning.TILE; val wh = gw.map.height * Tuning.TILE
            val sectors = HashSet<Int>()
            with(gw.world) {
                gw.world.family { all(Mob, Transform) }.forEach { e ->
                    if (e[Mob].def.lifeKind != LifeKind.WILDLIFE) return@forEach
                    val t = e[Transform]
                    val gx = (t.x / (ww / 3f)).toInt().coerceIn(0, 2)
                    val gy = (t.y / (wh / 3f)).toInt().coerceIn(0, 2)
                    sectors.add(gy * 3 + gx)
                }
            }
            assertTrue(sectors.size >= 8, "seed $seed: fish spread across the sky (${sectors.size}/9 sectors)")
        }
    }

    @Test fun `the tyrant shark is a brave apex that hunts sapients — and bites the keeper`() {
        val defs = GameConfig().enemies
        val shark = defs.getValue("tyrant_shark")
        assertEquals(LifeKind.WILDLIFE, shark.lifeKind)
        assertEquals(WildRole.APEX, shark.wildRole)
        assertTrue(shark.bravery >= Predation.BRAVE, "the tyrant fears nothing")
        assertTrue(Predation.canPredate(shark, defs.getValue("spore_shaman")), "sapient adults are its prey")
        assertTrue(Predation.canPredate(shark, defs.getValue("void_shark")), "lesser hunters too")
        assertEquals(CreatureLook.Form.FISH, CreatureLook.of("tyrant_shark", shark.wildRole).form)

        val gw = WorldFactory.create(InputState(), seed = 6L)
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        MobFactory.spawn(gw.world, shark, px + 20f, py)
        val hpBefore = with(gw.world) { gw.player[Health].hp }
        repeat(60) { gw.world.update(dt) } // the lunge telegraphs first (v2.138), then the teeth close
        assertTrue(with(gw.world) { gw.player[Health].hp } < hpBefore, "the tyrant's bite lands on the keeper")
    }

    @Test fun `the pilot fish keep to their whale`() {
        val defs = GameConfig().enemies
        val whaleDef = defs.getValue("isle_whale")
        val pilotDef = defs.getValue("pilot_minnow")
        assertEquals(WildRole.HERD, whaleDef.wildRole, "the island is gentle — its retinue must not flee it")
        assertEquals(WildRole.SCHOOL, pilotDef.wildRole)
        assertEquals(CreatureLook.Form.FISH, CreatureLook.of("isle_whale", whaleDef.wildRole).form)

        val gw = WorldFactory.create(InputState(), seed = 9L)
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        val whale = MobFactory.spawn(gw.world, whaleDef, px + 900f, py)
        val pilots = (0 until 16).map { i ->
            val a = i * 0.3927f
            MobFactory.spawn(gw.world, pilotDef, px + 900f + cos(a) * 320f, py + sin(a) * 320f)
        }
        repeat(300) { gw.world.update(dt) } // five seconds of open water
        val (wx, wy) = with(gw.world) { whale[Transform].let { it.x to it.y } }
        val mean = with(gw.world) { pilots.map { e -> hypot(e[Transform].x - wx, e[Transform].y - wy).toDouble() }.average() }
        assertTrue(mean < 220.0, "the retinue closes on its whale (mean $mean)")
    }
}
