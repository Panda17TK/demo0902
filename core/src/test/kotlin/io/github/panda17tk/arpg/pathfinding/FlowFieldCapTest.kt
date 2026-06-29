package io.github.panda17tk.arpg.pathfinding

import io.github.panda17tk.arpg.map.TileMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FlowFieldCapTest {
    @Test fun `rebuild does not reach tiles beyond maxDist`() {
        val map = TileMap.fromRows(List(5) { ".".repeat(20) }) // open floor, no walls
        val f = FlowField(map.width, map.height)
        f.rebuild(map, 0, 0, maxDist = 3)
        assertEquals(0, f.distAt(0, 0))
        assertTrue(f.distAt(3, 0) <= 3)
        assertEquals(FlowField.UNREACHABLE, f.distAt(10, 0))
    }
}
