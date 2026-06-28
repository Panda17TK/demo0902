package io.github.panda17tk.arpg.map

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TileMapTest {
    private fun map() = TileMap.fromRows(listOf("###", "#.#", "###"))

    @Test fun `dimensions match rows`() {
        val m = map(); assertEquals(3, m.width); assertEquals(3, m.height)
    }
    @Test fun `floor center is not solid, walls are solid`() {
        val m = map()
        assertFalse(m.solidAt(1, 1)); assertTrue(m.solidAt(0, 0)); assertTrue(m.solidAt(1, 0))
    }
    @Test fun `out of bounds is solid`() {
        val m = map()
        assertTrue(m.solidAt(-1, 0)); assertTrue(m.solidAt(3, 3))
    }
    @Test fun `door is solid`() {
        val m = TileMap.fromRows(listOf("DDD", "D.D", "DDD"))
        assertTrue(m.solidAt(0, 0)); assertEquals(Tile.DOOR, m.tileAt(0, 0))
    }
}
