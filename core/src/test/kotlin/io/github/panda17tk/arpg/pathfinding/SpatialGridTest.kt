package io.github.panda17tk.arpg.pathfinding

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SpatialGridTest {
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
