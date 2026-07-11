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
            // v2.169: every slice keeps the gate's GLOBAL bearing, for the compass home
            val gg = gw.worldState.gateGlobal
            assertTrue(gg != null, "($ax,$ay) knows where the gate is")
            gw.worldState.gate?.let { g ->
                val dx = g.first - (gg!!.first - gw.worldState.areaOriginX)
                val dy = g.second - (gg.second - gw.worldState.areaOriginY)
                assertTrue(dx * dx + dy * dy < (Tuning.TILE * 9f) * (Tuning.TILE * 9f), "the local gate sits on the global plan")
            }
        }
        assertEquals(full.planets.map { it.id }.toSet(), union.keys, "every planet of the sky lands in a slice")
        assertTrue(union.values.all { it == 1 }, "…in exactly one slice")
        assertEquals(1, gates, "one jump gate in the whole sky")
        val centre = WorldFactory.create(InputState(), seed = 5L, area = 1 to 1)
        assertTrue(centre.worldState.gate != null, "…and it anchors the centre, where runs begin")
        assertTrue(centre.worldState.wrecks.isNotEmpty(), "the wrecks huddle near the anchor too")
    }

    @Test fun `sliceDims tiles the stage without building it`() {
        for (g in Stages.SPACE_GEN) {
            var sw = 0; var sh = 0
            for (i in 0 until Stages.AREA_N) {
                sw += Stages.sliceDims(g.id, i, 0).first
                sh += Stages.sliceDims(g.id, 0, i).second
            }
            assertEquals(g.w, sw, "${'$'}{g.id}: the widths tile")
            assertEquals(g.h, sh, "${'$'}{g.id}: the heights tile")
        }
        // and it agrees with an actually-built last slice — the remainder-bearing one
        val id = Stages.SPACE_GEN[1].id
        val built = Stages.slice(id, Stages.AREA_N - 1, Stages.AREA_N - 1).rows
        val dims = Stages.sliceDims(id, Stages.AREA_N - 1, Stages.AREA_N - 1)
        assertEquals(dims.first, built[0].length)
        assertEquals(dims.second, built.size)
    }

    @Test fun `an area entry lands clear of the edge-trigger band`() {
        val id = Stages.randomSpaceId(Rng(5L))
        val trigger = io.github.panda17tk.arpg.sim.AreaGrid.EDGE_TRIGGER
        val inset = io.github.panda17tk.arpg.sim.AreaGrid.ENTRY_INSET
        for ((ax, ay) in listOf(0 to 1, 2 to 2)) {
            val (tw, th) = Stages.sliceDims(id, ax, ay)
            val w = tw * Tuning.TILE; val h = th * Tuning.TILE
            // entries from all four edges, exactly as crossArea computes them
            for (entry in listOf(inset to h / 2, (w - inset) to h / 2, w / 2 to inset, w / 2 to (h - inset))) {
                val gw = WorldFactory.create(InputState(), seed = 5L, area = ax to ay, playerSpawn = entry)
                val (px, py) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
                val mw = gw.map.width * Tuning.TILE; val mh = gw.map.height * Tuning.TILE
                assertTrue(
                    px > trigger && px < mw - trigger && py > trigger && py < mh - trigger,
                    "entry ${'$'}entry into (${'$'}ax,${'$'}ay) rests at (${'$'}px,${'$'}py) — inside the trigger band",
                )
            }
        }
    }

    @Test fun `emptied treasures stay empty on a rebuild`() {
        fun caches(gw: GameWorld): Int = with(gw.world) {
            var n = 0
            gw.world.family { all(io.github.panda17tk.arpg.ecs.components.Pickup, Transform) }.forEach { e ->
                val kind = e[io.github.panda17tk.arpg.ecs.components.Pickup].kind
                if (kind != "med" && !kind.startsWith("item:")) return@forEach
                val t = e[Transform]
                if (gw.worldState.wrecks.any { w ->
                        val dx = t.x - w.first; val dy = t.y - w.second
                        dx * dx + dy * dy < (Tuning.TILE * 3f) * (Tuning.TILE * 3f)
                    }
                ) n++
            }
            n
        }
        val fresh = WorldFactory.create(InputState(), seed = 5L, area = 1 to 1)
        val wrecks = fresh.worldState.wrecks
        assertTrue(wrecks.isNotEmpty(), "the centre slice keeps its wrecks")
        assertEquals(wrecks.size, fresh.worldState.wreckIndices.size, "each hull knows its sky-wide index")
        assertTrue(caches(fresh) >= wrecks.size * 2, "a fresh sky stocks every cache")
        val looted = WorldFactory.create(
            InputState(), seed = 5L, area = 1 to 1,
            lootedWrecks = fresh.worldState.wreckIndices.toSet(), survivorRescued = true,
        )
        assertEquals(0, caches(looted), "emptied caches stay empty")
        assertTrue(looted.worldState.survivorRescued, "the rescue holds across the rebuild")
        assertEquals(wrecks, looted.worldState.wrecks, "the hulls themselves still drift where they were")
    }

    @Test fun `a swept comet tail stays swept`() {
        var fresh: GameWorld? = null
        var seed = 0L
        for (s in 5L..14L) {
            val gw = WorldFactory.create(InputState(), seed = s, area = 1 to 1)
            if (gw.worldState.comet != null) { fresh = gw; seed = s; break }
        }
        val head = fresh?.worldState?.comet
        assertTrue(head != null, "one of the trial skies carries a comet")
        fun beads(gw: GameWorld): Int = with(gw.world) {
            var n = 0
            gw.world.family { all(io.github.panda17tk.arpg.ecs.components.Pickup, Transform) }.forEach { e ->
                if (e[io.github.panda17tk.arpg.ecs.components.Pickup].kind != "dust") return@forEach
                val t = e[Transform]
                val dx = t.x - head!!.first; val dy = t.y - head.second
                if (dx * dx + dy * dy < 400f * 400f) n++
            }
            n
        }
        val swept = WorldFactory.create(InputState(), seed = seed, area = 1 to 1, cometSwept = true)
        assertTrue(beads(fresh!!) > beads(swept), "the beads are gone from the swept tail")
        assertEquals(head, swept.worldState.comet, "the comet itself still rides")
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
