package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VisitedMapTest {
    @Test fun `a fresh map has nothing visited`() {
        val v = VisitedMap(10, 10)
        assertEquals(0, v.count)
        assertFalse(v.visited(3, 3))
    }

    @Test fun `mark with a radius fills the square around the tile`() {
        val v = VisitedMap(10, 10)
        v.mark(5, 5, radius = 1)
        assertEquals(9, v.count)
        assertTrue(v.visited(4, 4)); assertTrue(v.visited(6, 6)); assertTrue(v.visited(5, 5))
        assertFalse(v.visited(7, 5))
    }

    @Test fun `marks near the edge clip to the map`() {
        val v = VisitedMap(4, 4)
        v.mark(0, 0, radius = 2)
        assertEquals(9, v.count) // the 3x3 corner that fits
        v.mark(-5, -5, radius = 1) // fully outside → no-op
        assertEquals(9, v.count)
    }

    @Test fun `re-marking is idempotent`() {
        val v = VisitedMap(8, 8)
        v.mark(2, 2, 1); v.mark(2, 2, 1)
        assertEquals(9, v.count)
    }

    @Test fun `markRect fills the viewport rectangle, clipped to the map`() {
        val v = VisitedMap(10, 10)
        v.markRect(2, 3, 5, 4) // 4×2 rect
        assertEquals(8, v.count)
        assertTrue(v.visited(2, 3) && v.visited(5, 4))
        assertFalse(v.visited(1, 3))
        v.markRect(-5, -5, 20, 0) // clips to row 0
        assertTrue(v.visited(0, 0) && v.visited(9, 0))
        assertEquals(18, v.count)
    }

    @Test fun `forEachVisited walks exactly the marked tiles`() {
        val v = VisitedMap(6, 6)
        v.mark(1, 2); v.mark(4, 5)
        val seen = mutableListOf<Pair<Int, Int>>()
        v.forEachVisited { x, y -> seen.add(x to y) }
        assertEquals(setOf(1 to 2, 4 to 5), seen.toSet())
    }
}
