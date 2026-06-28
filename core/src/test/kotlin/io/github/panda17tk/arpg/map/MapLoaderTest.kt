package io.github.panda17tk.arpg.map

import io.github.panda17tk.arpg.sim.Tuning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MapLoaderTest {
    @Test fun `arena1 loads as 30x20 with the player spawned inside`() {
        val loaded = MapLoader.load(Stages.byId("arena1"))
        assertEquals(30, loaded.tileMap.width)
        assertEquals(20, loaded.tileMap.height)
        // player spawn is inside the bounds (not at the default center fallback border)
        val ptx = (loaded.playerSpawnX / Tuning.TILE).toInt()
        val pty = (loaded.playerSpawnY / Tuning.TILE).toInt()
        assertFalse(loaded.tileMap.solidAt(ptx, pty), "player must spawn on floor")
    }
    @Test fun `border walls are indestructible, internal walls have finite HP`() {
        val loaded = MapLoader.load(Stages.byId("arena1"))
        val m = loaded.tileMap
        assertTrue(m.hp[m.index(0, 0)].isInfinite(), "border wall must be ∞ HP")
        // find one internal wall and assert finite 90 HP
        var foundFinite = false
        for (ty in 1 until m.height - 1) for (tx in 1 until m.width - 1) {
            if (m.tileAt(tx, ty) == Tile.WALL) { assertEquals(90f, m.hp[m.index(tx, ty)], 1e-3f); foundFinite = true }
        }
        assertTrue(foundFinite, "arena1 should have internal destructible walls")
    }
    @Test fun `damaging an internal wall to zero breaks it to floor`() {
        val loaded = MapLoader.load(Stages.byId("arena1"))
        val m = loaded.tileMap
        var wx = -1; var wy = -1
        loop@ for (ty in 1 until m.height - 1) for (tx in 1 until m.width - 1) {
            if (m.tileAt(tx, ty) == Tile.WALL && m.hp[m.index(tx, ty)].isFinite()) { wx = tx; wy = ty; break@loop }
        }
        val res = Tiles.damageTile(m, wx, wy, 1000f)
        assertTrue(res.broke); assertEquals(Tile.FLOOR, m.tileAt(wx, wy))
    }
}
