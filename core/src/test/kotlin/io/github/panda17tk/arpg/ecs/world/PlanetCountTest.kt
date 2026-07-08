package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.134 星の数: more landable worlds — every sky carries 10..14 planets, and all of them can be landed on. */
class PlanetCountTest {
    @Test fun `every sky carries 10 to 14 landable planets`() {
        for (seed in 1L..6L) {
            val gw = WorldFactory.create(InputState(), seed = seed)
            assertTrue(gw.planets.size in 10..14, "seed $seed: got ${gw.planets.size} planets")
            assertEquals(gw.planets.size, gw.planets.map { it.id }.toSet().size, "ids stay distinct")
            // every planet is a landing candidate when hovered — the landable set IS the planet set
            for (p in gw.planets) {
                val cand = io.github.panda17tk.arpg.sim.Landing.nearestLandable(p.cx, p.cy - p.radius, gw.planets, 64f)
                assertTrue(cand != null, "seed $seed: a planet at (${p.cx},${p.cy}) must accept a landing")
            }
        }
    }
}
