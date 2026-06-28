package io.github.panda17tk.arpg.ai

import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.pathfinding.FlowField
import io.github.panda17tk.arpg.sim.Tuning
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AiMoveTest {
    @Test fun `mob follows the flow-field toward the player`() {
        val m = TileMap.fromRows(listOf("........", "........", "........"))
        val ff = FlowField(m.width, m.height)
        // player at tile (7,1): flow distance decreases toward x=7
        ff.rebuild(m, startTileX = 7, startTileY = 1)
        // mob at world (16,48) = tile (0,1); should want to move +x (toward player)
        val dir = AiMove.followDir(m, ff, mobX = 16f, mobY = 48f)
        assertTrue(dir.first > 0.5f, "expected +x follow, got ${dir.first}")
    }
    @Test fun `mob with no downhill neighbour returns zero`() {
        val m = TileMap.fromRows(listOf("###", "#.#", "###"))
        val ff = FlowField(m.width, m.height)
        ff.rebuild(m, startTileX = 1, startTileY = 1)
        // mob sits on the source tile; no lower neighbour
        val dir = AiMove.followDir(m, ff, mobX = 1 * Tuning.TILE + 16f, mobY = 1 * Tuning.TILE + 16f)
        assertTrue(dir.first == 0f && dir.second == 0f)
    }
}
