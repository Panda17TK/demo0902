package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.map.Stages
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.sim.Tuning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.166 宙域の九分割: nine slices, one shared sky — each a ninth of the tiles and the ocean. */
class AreaWorldTest {
    @Test fun `the slices tile the stage and their rocks match the full grid`() {
        val id = Stages.randomSpaceId(Rng(9L))
        val (fw, fh) = Stages.spaceFullDims(id)
        var sumW = 0; var sumH = 0
        for (i in 0 until Stages.AREA_N) {
            sumW += Stages.slice(id, i, 0).rows[0].length
            sumH += Stages.slice(id, 0, i).rows.size
        }
        assertEquals(fw, sumW, "the columns tile the full width")
        assertEquals(fh, sumH, "the rows tile the full height")
        // rock parity: inside a slice (off its ring), every '#' matches the full stage's cell
        val full = Stages.byId(id).rows
        val (ox, oy) = Stages.sliceOrigin(id, 1, 1)
        val s = Stages.slice(id, 1, 1).rows
        var checked = 0
        for (ly in 1 until s.size - 1 step 37) {
            for (lx in 1 until s[ly].length - 1 step 23) {
                val a = s[ly][lx] == '#'
                val b = full[oy + ly][ox + lx] == '#'
                assertEquals(b, a, "rock parity at local ($lx,$ly)")
                checked++
            }
        }
        assertTrue(checked > 100, "the parity sweep covered real ground ($checked cells)")
    }

    @Test fun `nine slices partition the planets of one shared sky`() {
        val full = WorldFactory.create(InputState(), seed = 5L)
        val union = HashMap<Long, Int>()
        var gates = 0
        for (ax in 0 until Stages.AREA_N) for (ay in 0 until Stages.AREA_N) {
            val gw = WorldFactory.create(InputState(), seed = 5L, area = ax to ay)
            assertTrue(gw.map.width <= full.map.width / 3 + 2, "a slice is a ninth (${gw.map.width} vs ${full.map.width})")
            assertEquals(ax, gw.worldState.areaX); assertEquals(ay, gw.worldState.areaY)
            val w = gw.map.width * Tuning.TILE; val h = gw.map.height * Tuning.TILE
            gw.planets.forEach { p ->
                union.merge(p.id, 1, Int::plus)
                assertTrue(p.cx in 0f..w && p.cy in 0f..h, "a slice's planet lives in local coordinates")
            }
            if (gw.worldState.gate != null) gates++
        }
        assertEquals(full.planets.map { it.id }.toSet(), union.keys, "every planet of the sky lands in a slice")
        assertTrue(union.values.all { it == 1 }, "…in exactly one slice")
        assertEquals(1, gates, "one jump gate in the whole sky")
        val centre = WorldFactory.create(InputState(), seed = 5L, area = 1 to 1)
        assertTrue(centre.worldState.gate != null, "…and it anchors the centre, where runs begin")
        assertTrue(centre.worldState.wrecks.isNotEmpty(), "the wrecks huddle near the anchor too")
    }

    @Test fun `an area's ocean is a fraction, deterministic, and stays inside its slice`() {
        fun fish(gw: GameWorld): List<Triple<String, Float, Float>> = buildList {
            with(gw.world) {
                gw.world.family { all(Mob, Transform) }.forEach { e ->
                    val m = e[Mob]
                    if (m.def.lifeKind != LifeKind.WILDLIFE) return@forEach
                    val t = e[Transform]
                    add(Triple(m.def.id, t.x, t.y))
                }
            }
        }
        val full = fish(WorldFactory.create(InputState(), seed = 7L))
        val a = WorldFactory.create(InputState(), seed = 7L, area = 0 to 2)
        val b = WorldFactory.create(InputState(), seed = 7L, area = 0 to 2)
        assertEquals(fish(a), fish(b), "same slice, same ocean")
        val mine = fish(a)
        assertTrue(mine.isNotEmpty(), "the slice has its share of the ocean")
        assertTrue(mine.size < full.size / 3, "…and only its share (${mine.size} of ${full.size})")
        val w = a.map.width * Tuning.TILE; val h = a.map.height * Tuning.TILE
        assertTrue(mine.all { it.second in 0f..w && it.third in 0f..h }, "every fish swims inside the slice")
    }
}
