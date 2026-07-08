package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.ecs.components.MobAction
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.hypot

/** v2.131 大海の宙: 32 more fish — boid schools near a hundred strong, and blink-swimmers. */
class WorldSchoolTest {
    private val dt = 1f / 60f

    /** Find a seed whose sky hosts the tiny boid school. */
    private fun fishSky(): GameWorld {
        for (seed in 1L..12L) {
            val gw = WorldFactory.create(InputState(), seed = seed)
            var n = 0
            with(gw.world) { gw.world.family { all(Mob) }.forEach { if (it[Mob].def.wildRole == WildRole.SCHOOL) n++ } }
            if (n >= 80) return gw
        }
        error("no fish sky in seeds 1..12")
    }

    @Test fun `the tiny school runs near a hundred fish`() {
        val gw = fishSky()
        var school = 0
        with(gw.world) { gw.world.family { all(Mob) }.forEach { if (it[Mob].def.wildRole == WildRole.SCHOOL) school++ } }
        assertTrue(school in 80..140, "a school near a hundred strong (got $school)")
    }

    @Test fun `the boids hold the school together and align its swim`() {
        val gw = fishSky()
        repeat(300) { gw.world.update(dt) } // five seconds of open water
        val xs = ArrayList<Float>(); val ys = ArrayList<Float>()
        val hx = ArrayList<Float>(); val hy = ArrayList<Float>()
        with(gw.world) {
            gw.world.family { all(Mob) }.forEach { e ->
                if (e[Mob].def.wildRole != WildRole.SCHOOL) return@forEach
                val t = e[Transform]; xs.add(t.x); ys.add(t.y)
                val f = e[io.github.panda17tk.arpg.ecs.components.Facing]; hx.add(f.x); hy.add(f.y)
            }
        }
        assertTrue(xs.size >= 60, "the school survives open water (got ${xs.size})")
        val cx = xs.average().toFloat(); val cy = ys.average().toFloat()
        val within = xs.indices.count { hypot(xs[it] - cx, ys[it] - cy) < 240f }
        assertTrue(within >= xs.size * 3 / 4, "cohesion keeps most fish near the centroid ($within/${xs.size})")
        // alignment: the average heading has real length only when the school swims together
        val mx = hx.average().toFloat(); val my = hy.average().toFloat()
        assertTrue(hypot(mx, my) > 0.25f, "the school swims as one (mean heading ${hypot(mx, my)})")
    }

    @Test fun `a blink-swimmer vanishes a stride when frightened`() {
        val gw = WorldFactory.create(InputState(), seed = 2L)
        val def = GameConfig().enemies.getValue("blink_darter")
        assertTrue(def.canBlink && def.lifeKind == LifeKind.WILDLIFE)
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        val fish = MobFactory.spawn(gw.world, def, px + 50f, py)
        repeat(40) { gw.world.update(dt) }
        val a = with(gw.world) { fish[MobAction] }
        assertTrue(a.dodgeCd > 0f, "the flee set the blink (dodgeCd doubles as its cooldown)")
    }

    @Test fun `the fish roster spans thirty-five species and the teeth hunt the schools`() {
        val defs = GameConfig().enemies
        val fishForms = defs.filterValues { it.lifeKind == LifeKind.WILDLIFE }
            .count { (id, d) -> io.github.panda17tk.arpg.render.CreatureLook.of(id, d.wildRole).form == io.github.panda17tk.arpg.render.CreatureLook.Form.FISH }
        assertEquals(35, fishForms, "3 (v2.130) + 32 (v2.131)")
        assertTrue(io.github.panda17tk.arpg.sim.Predation.canPredate(defs.getValue("void_shark"), defs.getValue("star_sardine")),
            "a shark hunts the sardine school")
    }
}
