package io.github.panda17tk.arpg.pathfinding

import io.github.panda17tk.arpg.map.TileMap
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LosTest {
    @Test fun `clear line has line of sight`() {
        val m = TileMap.fromRows(listOf(".....", ".....", "....."))
        assertTrue(Los.hasLineOfSight(m, 16f, 16f, 144f, 16f)) // across open row 0
    }
    @Test fun `wall blocks line of sight`() {
        val m = TileMap.fromRows(listOf(".....", "..#..", "....."))
        assertFalse(Los.hasLineOfSight(m, 16f, 48f, 144f, 48f)) // row 1 has a wall at tile (2,1)
    }
}
