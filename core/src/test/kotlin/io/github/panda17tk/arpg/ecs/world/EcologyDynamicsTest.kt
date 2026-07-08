package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.MobAction
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * v2.132 生態系の力学, live-world half: an apex actually bites the keeper, a blink-hunter
 * closes a chase in one stride, and a hunter's kill of a sapient credits no one.
 */
class EcologyDynamicsTest {
    private val dt = 1f / 60f

    @Test fun `an apex in its wrath bites the keeper`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        val defs = GameConfig().enemies
        val apex = defs.getValue("forest_apex")
        assertTrue(apex.bravery >= io.github.panda17tk.arpg.sim.Predation.BRAVE, "the apex is brave")
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        MobFactory.spawn(gw.world, apex, px + 20f, py)
        val hpBefore = with(gw.world) { gw.player[Health].hp }
        repeat(10) { gw.world.update(dt) }
        val hpAfter = with(gw.world) { gw.player[Health].hp }
        assertTrue(hpAfter < hpBefore, "the bite lands ($hpBefore -> $hpAfter)")
    }

    @Test fun `a timid hunter never bites the keeper`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        val timid = GameConfig().enemies.getValue("forest_apex").copy(bravery = 0.2f)
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        MobFactory.spawn(gw.world, timid, px + 20f, py)
        val hpBefore = with(gw.world) { gw.player[Health].hp }
        repeat(10) { gw.world.update(dt) }
        val hpAfter = with(gw.world) { gw.player[Health].hp }
        assertEquals(hpBefore, hpAfter, "a timid beast only threatens")
    }

    @Test fun `a blink-hunter closes the last stretch of a chase in one stride`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        val defs = GameConfig().enemies
        val lynxDef = defs.getValue("bramble_lynx")
        assertTrue(lynxDef.canBlink && lynxDef.wildRole == WildRole.PREDATOR)
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        // far from the keeper so only the deer drives the chase
        val lynx = MobFactory.spawn(gw.world, lynxDef, px + 600f, py)
        MobFactory.spawn(gw.world, defs.getValue("horn_deer"), px + 750f, py)
        var fired = false
        repeat(60) {
            with(gw.world) { lynx[Mob].hunger = 1f } // keep it on the hunt
            gw.world.update(dt)
            with(gw.world) { if (lynx[MobAction].dodgeCd > 0f) fired = true }
        }
        assertTrue(fired, "the chase triggered the blink (dodgeCd doubles as its cooldown)")
    }

    @Test fun `a hunter's kill of a sapient credits no one`() {
        val gw = WorldFactory.create(InputState(), seed = 4L)
        val soldier = GameConfig().enemies.getValue("lost_soldier")
        assertEquals(LifeKind.SAPIENT, soldier.lifeKind)
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        val fallen = MobFactory.spawn(gw.world, soldier, px + 400f, py)
        val killsBefore = gw.gameOver.kills
        with(gw.world) { fallen[Mob].fellByWild = true; fallen[Health].hp = 0f }
        gw.world.update(dt)
        assertEquals(killsBefore, gw.gameOver.kills, "the wild's deed ticks no score")
        assertEquals(null, gw.gameOver.killsByKind["lost_soldier"], "and enters no field book")
    }
}
