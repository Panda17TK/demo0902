package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.map.TileMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CollisionTest {
    // 3x3 map, TILE=32 -> world 96x96; center cell (1,1) is the only floor.
    private fun walledRoom() = TileMap.fromRows(listOf("###", "#.#", "###"))

    @Test fun `free move in open space changes position`() {
        val open = TileMap.fromRows(listOf(".....", ".....", "....."))
        val r = Collision.moveAndCollide(open, x = 48f, y = 48f, halfW = 11f, halfH = 11f, dx = 10f, dy = 0f)
        assertEquals(58f, r.x, 1e-3f); assertEquals(48f, r.y, 1e-3f)
    }
    @Test fun `moving right into the wall stops at the wall face`() {
        val m = walledRoom()
        // start centered in cell (1,1) = world (48,48); push hard right into wall at x>=64.
        val r = Collision.moveAndCollide(m, x = 48f, y = 48f, halfW = 11f, halfH = 11f, dx = 100f, dy = 0f)
        // right wall left face is x=64; entity right edge clamps there -> x = 64 - 11 = 53
        assertEquals(53f, r.x, 1e-3f); assertTrue(r.hitX)
    }
    @Test fun `moving up into the wall stops at the wall face`() {
        val m = walledRoom()
        val r = Collision.moveAndCollide(m, x = 48f, y = 48f, halfW = 11f, halfH = 11f, dx = 0f, dy = -100f)
        // top wall bottom face is y=32; entity top edge clamps -> y = 32 + 11 = 43
        assertEquals(43f, r.y, 1e-3f); assertTrue(r.hitY)
    }
}
