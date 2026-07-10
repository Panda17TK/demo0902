package io.github.panda17tk.arpg.pathfinding

import io.github.panda17tk.arpg.map.TileMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** v2.139 描画の倹約: epoch stamps replace the full-array clear — old epochs must read as unreachable. */
class FlowFieldEpochTest {
    @Test fun `a rebuild from a new source invalidates every cell of the old field`() {
        val m = TileMap.fromRows(listOf(".....", ".###.", ".....")) // two rooms joined at the edges
        val ff = FlowField(m.width, m.height)
        ff.rebuild(m, startTileX = 0, startTileY = 0, maxDist = 1)
        assertEquals(0, ff.distAt(0, 0))
        assertEquals(1, ff.distAt(1, 0))
        assertEquals(FlowField.UNREACHABLE, ff.distAt(4, 2), "beyond the cap")

        ff.rebuild(m, startTileX = 4, startTileY = 2, maxDist = 1)
        assertEquals(0, ff.distAt(4, 2))
        assertEquals(1, ff.distAt(3, 2))
        // the OLD field's cells must not leak through the un-cleared array
        assertEquals(FlowField.UNREACHABLE, ff.distAt(0, 0), "the previous epoch is gone")
        assertEquals(FlowField.UNREACHABLE, ff.distAt(1, 0), "the previous epoch is gone")
    }

    @Test fun `before any rebuild the field is unreachable everywhere`() {
        val m = TileMap.fromRows(listOf("..", ".."))
        val ff = FlowField(m.width, m.height)
        assertEquals(FlowField.UNREACHABLE, ff.distAt(0, 0))
        assertEquals(FlowField.UNREACHABLE, ff.distAt(1, 1))
    }
}
