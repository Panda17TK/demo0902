package io.github.panda17tk.arpg.pathfinding

import io.github.panda17tk.arpg.map.TileMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FlowFieldTest {
    @Test fun `distance grows by one per step from the source on open floor`() {
        val m = TileMap.fromRows(listOf(".....", ".....", "....."))
        val ff = FlowField(m.width, m.height)
        ff.rebuild(m, startTileX = 0, startTileY = 0)
        assertEquals(0, ff.distAt(0, 0))
        assertEquals(1, ff.distAt(1, 0))
        assertEquals(2, ff.distAt(2, 0))
        assertEquals(2, ff.distAt(1, 1)) // 4-neighbour BFS: (0,0)->(1,0)->(1,1)
    }
    @Test fun `walls are unreachable`() {
        val m = TileMap.fromRows(listOf("###", "#.#", "###"))
        val ff = FlowField(m.width, m.height)
        ff.rebuild(m, startTileX = 1, startTileY = 1)
        assertEquals(0, ff.distAt(1, 1))
        assertEquals(FlowField.UNREACHABLE, ff.distAt(0, 0))
    }
}
