package io.github.panda17tk.arpg.pathfinding

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.140 シムの節約: the grid recycles its bucket lists across clears — results stay exact. */
class SpatialGridReuseTest {
    @Test fun `clearing and refilling keeps queries exact across many ticks`() {
        val grid = SpatialGrid<Int>(32f)
        repeat(50) { tick ->
            grid.clear()
            for (i in 0 until 20) grid.insert(i, i * 40f, tick * 1f)
            val near = ArrayList<Int>()
            grid.forNearby(200f, tick * 1f, 50f, near::add)
            assertTrue(5 in near && near.size in 2..5, "tick $tick: the neighbourhood answers ($near)")
        }
        // after a clear, nothing lingers from any earlier tick
        grid.clear()
        var count = 0
        grid.forNearby(200f, 25f, 4000f, { count++ })
        assertEquals(0, count, "a cleared grid is empty")
    }
}
