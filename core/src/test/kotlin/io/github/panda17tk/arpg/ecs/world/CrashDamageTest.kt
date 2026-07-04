package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Crash damage rules (v2.36): a hard slam into a planet/wall costs 1..5 HP scaling with impact
 * speed; gentle bumps stay free. (Supersedes the old "collisions never deal damage" contract.)
 */
class CrashDamageTest {
    /** Build a space world with the player just inside the first planet's surface, driving inward at [speed]. */
    private fun slam(speed: Float): GameWorld {
        val gw = WorldFactory.create(InputState(), seed = 1L) // SPACE stage — has planets
        val planet = gw.planets.firstOrNull()
        assertTrue(planet != null, "the space stage should have planets")
        with(gw.world) {
            val t = gw.player[Transform]; val v = gw.player[Velocity]; val h = gw.player[Health]
            h.hp = h.hpMax; h.iTime = 0f
            t.x = planet!!.cx + planet.radius - 2f; t.y = planet.cy
            t.prevX = t.x; t.prevY = t.y
            v.driftX = -speed
        }
        gw.world.update(1f / 60f)
        return gw
    }

    private fun lost(gw: GameWorld): Float = with(gw.world) { gw.player[Health].hpMax - gw.player[Health].hp }

    @Test fun `a gentle bump costs nothing`() {
        assertEquals(0f, lost(slam(280f)), 1e-3f) // below the 320 damage threshold (shake only)
    }

    @Test fun `a mid-speed crash costs about 1 HP`() {
        val l = lost(slam(500f)) // (500-320) * 0.008 = 1.44
        assertTrue(l in 1f..2f, "expected ~1.4 HP lost, got $l")
    }

    @Test fun `even the hardest slam is capped at 5 HP`() {
        assertEquals(5f, lost(slam(6000f)), 1e-3f)
    }

    @Test fun `crash-proof gear eats the slam entirely`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        val planet = gw.planets.firstOrNull()
        assertTrue(planet != null)
        with(gw.world) {
            gw.player[io.github.panda17tk.arpg.ecs.components.Gear].loadout.set(
                io.github.panda17tk.arpg.item.EquipSlot.ARMOR,
                io.github.panda17tk.arpg.item.ItemCatalog.byId("armor_shock")!!,
            )
            val t = gw.player[Transform]; val v = gw.player[Velocity]; val h = gw.player[Health]
            h.hp = h.hpMax; h.iTime = 0f
            t.x = planet!!.cx + planet.radius - 2f; t.y = planet.cy
            t.prevX = t.x; t.prevY = t.y
            v.driftX = -6000f
        }
        gw.world.update(1f / 60f)
        assertEquals(0f, lost(gw), 1e-3f)
    }
}
