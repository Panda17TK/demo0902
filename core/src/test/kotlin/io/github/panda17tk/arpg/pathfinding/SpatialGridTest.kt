package io.github.panda17tk.arpg.pathfinding

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SpatialGridTest {
    @Test fun `open addressing matches cell semantics over random points and reuse cycles`() {
        // v2.176 GCの静音化: the primitive-key table must behave exactly like the old HashMap —
        // same cells, same per-bucket order, across clear()/reuse cycles and growth.
        val cellSize = 32f
        val g = SpatialGrid<Int>(cellSize)
        val rng = io.github.panda17tk.arpg.math.Rng(7L)
        val pts = List(3000) { Triple(it, rng.nextFloat() * 4000f - 500f, rng.nextFloat() * 4000f - 500f) }
        fun cellOf(v: Float) = kotlin.math.floor(v / cellSize).toInt()
        repeat(3) {
            g.clear()
            for ((id, x, y) in pts) g.insert(id, x, y)
            val qx = 1200f; val qy = 900f; val r = 300f
            val got = ArrayList<Int>()
            g.forNearby(qx, qy, r) { got.add(it) }
            val want = pts.filter { (_, x, y) ->
                cellOf(x) in cellOf(qx - r)..cellOf(qx + r) && cellOf(y) in cellOf(qy - r)..cellOf(qy + r)
            }.map { it.first }
            org.junit.jupiter.api.Assertions.assertEquals(want.toSet(), got.toSet(), "same members")
            org.junit.jupiter.api.Assertions.assertEquals(got.size, got.toSet().size, "no duplicates")
        }
    }

    @Test fun `nearby query returns items within radius cells`() {
        val grid = SpatialGrid<String>(cell = 32f)
        grid.insert("a", 10f, 10f)
        grid.insert("b", 20f, 15f)
        grid.insert("far", 5000f, 5000f)
        val found = mutableListOf<String>()
        grid.forNearby(12f, 12f, radius = 20f) { found.add(it) }
        assertTrue(found.contains("a")); assertTrue(found.contains("b"))
        assertEquals(false, found.contains("far"))
    }
    @Test fun `clear empties the grid`() {
        val grid = SpatialGrid<String>(cell = 32f)
        grid.insert("a", 10f, 10f)
        grid.clear()
        val found = mutableListOf<String>()
        grid.forNearby(10f, 10f, 32f) { found.add(it) }
        assertEquals(0, found.size)
    }
}
