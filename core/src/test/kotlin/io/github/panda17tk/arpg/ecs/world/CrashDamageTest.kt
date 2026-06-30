package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Regression for the self-destruct bug: collisions push out and rebound, but never deal crash/fall damage. */
class CrashDamageTest {
    @Test fun `slamming the player into a planet deals no crash damage`() {
        val gw = WorldFactory.create(InputState(), seed = 1L) // SPACE stage — has planets
        val planet = gw.planets.firstOrNull()
        assertTrue(planet != null, "the space stage should have planets")
        with(gw.world) {
            val t = gw.player[Transform]; val v = gw.player[Velocity]; val h = gw.player[Health]
            h.hp = h.hpMax; h.iTime = 0f // vulnerable, so old crash damage would have landed
            // drop the player just inside the planet surface, driving hard toward its centre
            t.x = planet!!.cx + planet.radius - 2f; t.y = planet.cy
            t.prevX = t.x; t.prevY = t.y
            v.driftX = -6000f
        }
        gw.world.update(1f / 60f)
        with(gw.world) {
            val h = gw.player[Health]
            assertEquals(h.hpMax, h.hp, 1e-3f) // no HP lost to the impact
        }
    }
}
