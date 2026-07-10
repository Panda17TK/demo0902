package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.sim.Predation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.138 公正な野生: the escort counts the keeper's own kills, and the timid majority keeps the old line. */
class EcologyFairnessTest {
    private val dt = 1f / 60f

    @Test fun `the escort quest counts the keeper's kills - never the ecosystem's own`() {
        val gw = WorldFactory.create(InputState(), seed = 4L)
        val wolf = GameConfig().enemies.getValue("fang_wolf")
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        val before = gw.worldState.questPredators

        val hunted = MobFactory.spawn(gw.world, wolf, px + 400f, py)
        with(gw.world) { hunted[Mob].fellByWild = true; hunted[Health].hp = 0f } // an apex's kill
        gw.world.update(dt)
        assertEquals(before, gw.worldState.questPredators, "the food web must not fulfil the escort")

        val mine = MobFactory.spawn(gw.world, wolf, px + 400f, py)
        with(gw.world) { mine[Health].hp = 0f } // the keeper's own kill
        gw.world.update(dt)
        assertEquals(before + 1, gw.worldState.questPredators, "the keeper's kill counts")
    }

    @Test fun `the timid majority keeps the old line - the brave few cross it`() {
        val defs = GameConfig().enemies
        val hunters = defs.values.filter {
            it.lifeKind == LifeKind.WILDLIFE && (it.wildRole == WildRole.PREDATOR || it.wildRole == WildRole.APEX)
        }
        val brave = hunters.count { it.bravery >= Predation.BRAVE }
        val timid = hunters.size - brave
        assertTrue(brave >= 5, "some hunters still bite the keeper (got $brave)")
        assertTrue(timid >= brave, "but the timid majority holds ($timid timid vs $brave brave)")
        assertTrue(defs.getValue("fang_wolf").bravery < Predation.BRAVE, "the wolf threatens, never bites")
        assertTrue(defs.getValue("gravity_whale").bravery < Predation.BRAVE, "the whale is gentle")
        assertTrue(defs.getValue("white_stalker").bravery >= Predation.BRAVE, "the pale hunter crosses the line")
        assertTrue(defs.getValue("tyrant_shark").bravery >= Predation.BRAVE, "the tyrant fears nothing")
    }
}
