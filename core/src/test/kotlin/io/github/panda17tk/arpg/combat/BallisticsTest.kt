package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.map.TileMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BallisticsTest {
    @Test fun `bullet moves and survives over open floor`() {
        val m = TileMap.fromRows(listOf("..........", "..........", ".........."))
        val s = Ballistics.stepBullet(m, x = 16f, y = 48f, vx = 360f, vy = 0f, life = 0.9f, dt = 0.1f)
        assertEquals(52f, s.x, 1e-3f); assertNull(s.wallTile); assertFalse(s.expired)
    }
    @Test fun `bullet entering a wall reports the wall tile`() {
        val m = TileMap.fromRows(listOf("..#.......", "..#.......", "..#......."))
        val s = Ballistics.stepBullet(m, x = 60f, y = 48f, vx = 360f, vy = 0f, life = 0.9f, dt = 0.05f)
        assertNotNull(s.wallTile) // crossed into tile x=2 (world 64..96)
        assertEquals(2, s.wallTile!!.first)
    }
    @Test fun `bullet expires when life runs out`() {
        val m = TileMap.fromRows(listOf("..........", "..........", ".........."))
        val s = Ballistics.stepBullet(m, x = 16f, y = 48f, vx = 0f, vy = 0f, life = 0.05f, dt = 0.1f)
        assertTrue(s.expired)
    }
}
