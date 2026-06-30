package io.github.panda17tk.arpg.ecs.world

import com.github.quillraven.fleks.Entity
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.config.WildState
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.WorldMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.hypot

/** Nest behaviour now uses Mob.homeX/homeY: ReturnNest heads home (not the herd centre); GuardNest holds by it. */
class WildlifeSystemNestTest {
    private fun world() = WorldFactory.create(InputState(), seed = 3L, mode = WorldMode.SURFACE, biome = PlanetBiome.NATURE)
    private fun find(gw: GameWorld, pred: (Mob) -> Boolean): Entity? {
        var found: Entity? = null
        gw.world.family { all(Mob, Transform) }.forEach { e -> if (found == null && with(gw.world) { pred(e[Mob]) }) found = e }
        return found
    }
    private fun movePlayer(gw: GameWorld, x: Float, y: Float) = with(gw.world) {
        gw.world.family { all(PlayerTag, Transform) }.forEach { val t = it[Transform]; t.x = x; t.y = y; t.prevX = x; t.prevY = y }
    }
    private fun nestGuard(gw: GameWorld) = find(gw) { it.def.lifeKind == LifeKind.WILDLIFE && it.def.wildRole == WildRole.NEST_GUARD }

    @Test fun `a strayed nest-guard heads back toward its home`() {
        val gw = world()
        val guard = nestGuard(gw); assertNotNull(guard)
        movePlayer(gw, 40000f, 40000f) // far away
        with(gw.world) {
            val m = guard!![Mob]; val t = guard[Transform]
            t.x = m.homeX + 400f; t.y = m.homeY; t.prevX = t.x; t.prevY = t.y // strayed far to the +x of home
        }
        gw.world.update(1f / 60f)
        with(gw.world) {
            val m = guard!![Mob]; val f = guard[Facing]
            assertTrue(m.wildState == WildState.ReturnNest || m.wildState == WildState.GuardNest, "nest-tending state, was ${m.wildState}")
            assertTrue(f.x < 0f, "should head back toward home (-x), facing.x was ${f.x}")
        }
    }

    @Test fun `a nest-guard holds by its nest when threatened`() {
        val gw = world()
        val guard = nestGuard(gw); assertNotNull(guard)
        val (hx, hy) = with(gw.world) { val m = guard!![Mob]; m.homeX to m.homeY }
        with(gw.world) { val t = guard!![Transform]; t.x = hx; t.y = hy; t.prevX = hx; t.prevY = hy }
        movePlayer(gw, hx + 80f, hy) // an intruder inside the territory → GuardNest
        repeat(30) { gw.world.update(1f / 60f) }
        with(gw.world) {
            val t = guard!![Transform]
            assertTrue(hypot(t.x - hx, t.y - hy) < 60f, "the guard should hold by its nest: ${hypot(t.x - hx, t.y - hy)}")
        }
    }

    @Test fun `a hatchling flees a close player`() {
        val gw = world()
        val hatch = find(gw) { it.def.lifeKind == LifeKind.WILDLIFE && it.def.wildRole == WildRole.HATCHLING }; assertNotNull(hatch)
        val (hx, hy) = with(gw.world) { val t = hatch!![Transform]; t.x to t.y }
        movePlayer(gw, hx + 50f, hy) // well within flee range
        gw.world.update(1f / 60f)
        with(gw.world) { assertEquals(WildState.Flee, hatch!![Mob].wildState) }
    }
}
