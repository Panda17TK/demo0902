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

    /** The tiny boid school proper — the whale's pilot-fish retinue (v2.135) swims its own test. */
    private fun isTinySchool(id: String) = id == "star_sardine" || id == "void_aji"

    /** Tally the tiny-school flocks of a sky: flock id → member count (v2.144 大群衆). */
    private fun tinyFlocks(gw: GameWorld): Map<Int, Int> {
        val groups = HashMap<Int, Int>()
        with(gw.world) {
            gw.world.family { all(Mob) }.forEach { e ->
                val m = e[Mob]
                if (isTinySchool(m.def.id)) groups.merge(m.schoolGroup, 1, Int::plus)
            }
        }
        return groups
    }

    @Test fun `the tiny schools are many and each runs near a hundred fish`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        val flocks = tinyFlocks(gw)
        assertTrue(flocks.size >= 30, "v2.144 大群衆: the sky hosts many tiny flocks (got ${flocks.size})")
        assertTrue(flocks.values.sum() >= 2500, "thirtyfold waters (got ${flocks.values.sum()})")
        for ((g, n) in flocks) assertTrue(n in 60..140, "flock $g runs near a hundred strong (got $n)")
    }

    @Test fun `the boids hold a flock together and align its swim`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        // v2.164 軽い海: measure a flock swimming BESIDE the keeper — the full-rate boids the
        // player actually watches. The far ocean ticks on WildLod's coarser stagger by design;
        // that ring's "still swims, still deterministic" contract lives in WildLodTest.
        val def = GameConfig().enemies.getValue("star_sardine")
        val (px0, py0) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        val flock = 900101
        repeat(90) { i ->
            val a = i / 90f * 6.2831855f
            MobFactory.spawn(gw.world, def, px0 + 400f + kotlin.math.cos(a) * 60f, py0 + kotlin.math.sin(a) * 60f, schoolGroup = flock)
        }
        repeat(300) { gw.world.update(dt) } // five seconds of open water
        val xs = ArrayList<Float>(); val ys = ArrayList<Float>()
        val hx = ArrayList<Float>(); val hy = ArrayList<Float>()
        with(gw.world) {
            gw.world.family { all(Mob) }.forEach { e ->
                val m = e[Mob]
                if (!isTinySchool(m.def.id) || m.schoolGroup != flock) return@forEach
                val t = e[Transform]; xs.add(t.x); ys.add(t.y)
                val f = e[io.github.panda17tk.arpg.ecs.components.Facing]; hx.add(f.x); hy.add(f.y)
            }
        }
        assertTrue(xs.size >= 60, "the flock survives open water (got ${xs.size})")
        val cx = xs.average().toFloat(); val cy = ys.average().toFloat()
        val within = xs.indices.count { hypot(xs[it] - cx, ys[it] - cy) < 240f }
        assertTrue(within >= xs.size * 3 / 4, "cohesion keeps most fish near the centroid ($within/${xs.size})")
        // alignment: the average heading has real length only when the flock swims together
        val mx = hx.average().toFloat(); val my = hy.average().toFloat()
        assertTrue(hypot(mx, my) > 0.25f, "the flock swims as one (mean heading ${hypot(mx, my)})")
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
        assertEquals(38, fishForms, "3 (v2.130) + 32 (v2.131) + 3 (v2.135)")
        assertTrue(io.github.panda17tk.arpg.sim.Predation.canPredate(defs.getValue("void_shark"), defs.getValue("star_sardine")),
            "a shark hunts the sardine school")
    }
}
