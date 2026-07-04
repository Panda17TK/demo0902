package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.Cooldowns
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.WorldMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.hypot

/** v2.42: buffered manual fire, the surface shukuchi dash, and rank = capability. */
class WorldV242Test {
    @Test fun `a manual-fire release during a short cooldown is buffered, not swallowed`() {
        val input = InputState()
        val gw = WorldFactory.create(input, seed = 3L)
        with(gw.world) {
            val ars = gw.player[Arsenal]
            ars.curW = ars.weapons.indexOfFirst { it.def.id == "beam" }
            gw.player[Cooldowns].shoot = 0.2f // mid-cooldown when the release lands
        }
        input.fireRelease = true // the old edge would be lost here
        repeat(20) { gw.world.update(1f / 60f) } // ~0.33s: cooldown expires inside the buffer window
        with(gw.world) {
            assertTrue(gw.player[Cooldowns].shoot > 0.25f, "the buffered release should have fired the beam")
            assertEquals(0f, input.fireReleaseT, 1e-4f) // consumed by the shot
        }
    }

    @Test fun `under gravity the dash button is a shukuchi step`() {
        val input = InputState()
        val gw = WorldFactory.create(input, seed = 7L, mode = WorldMode.SURFACE, biome = PlanetBiome.NATURE)
        val (x0, y0) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
        val sta0 = with(gw.world) { gw.player[Stamina].value }
        input.dash = true
        gw.world.update(1f / 60f)
        val (x1, y1) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
        assertTrue(hypot(x1 - x0, y1 - y0) > 60f, "expected an instant step, moved ${hypot(x1 - x0, y1 - y0)}")
        assertTrue(with(gw.world) { gw.player[Stamina].value } < sta0, "the step should cost stamina")
        assertTrue(with(gw.world) { gw.player[Cooldowns].blink } > 0f, "the step should start its cooldown")
        // Holding the button does NOT chain steps — the next tick moves normally.
        val (x2, y2) = with(gw.world) { gw.world.update(1f / 60f); val t = gw.player[Transform]; t.x to t.y }
        assertTrue(hypot(x2 - x1, y2 - y1) < 20f, "holding dash must not teleport again")
    }

    @Test fun `in space the dash button stays a thruster, not a teleport`() {
        val input = InputState()
        val gw = WorldFactory.create(input, seed = 7L)
        val (x0, y0) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
        input.dash = true
        gw.world.update(1f / 60f)
        val (x1, y1) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
        assertTrue(hypot(x1 - x0, y1 - y0) < 10f, "space dash accelerates — it must not blink")
    }

    @Test fun `rank is capability - elites spawn at a level that unlocks their whole kit`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        val cfg = GameConfig()
        val boss = MobFactory.spawn(gw.world, cfg.enemies["overlord"]!!, 500f, 500f)
        val mid = MobFactory.spawn(gw.world, cfg.enemies["artillery"]!!, 600f, 600f)
        val grunt = MobFactory.spawn(gw.world, cfg.enemies["zombie"]!!, 700f, 700f)
        with(gw.world) {
            assertEquals(10, boss[Mob].level) // 11 attacks, all unlocked; smarts +0.9
            assertEquals(5, mid[Mob].level)
            assertEquals(1, grunt[Mob].level)
        }
    }

    @Test fun `the elite ranks now carry combat intelligence`() {
        val enemies = GameConfig().enemies
        assertTrue(enemies["overlord"]!!.intelligence >= 0.9f)
        assertTrue(enemies["warlock"]!!.intelligence >= 0.8f)
        assertTrue(enemies["overlord"]!!.attacks.size >= 11) // the throne's 11th move (spiral)
        assertTrue(enemies["brute"]!!.attacks.any { it.type == "shockwave" })
    }
}
