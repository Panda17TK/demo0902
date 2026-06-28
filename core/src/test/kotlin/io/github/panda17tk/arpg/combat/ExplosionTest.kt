package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.config.PlayerConfig
import io.github.panda17tk.arpg.map.MapLoader
import io.github.panda17tk.arpg.map.Stages
import io.github.panda17tk.arpg.map.Tile
import io.github.panda17tk.arpg.sim.Tuning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExplosionTest {
    @Test fun `falloff is full at center and zero at the radius edge`() {
        assertEquals(1f, Explosion.falloff(0f, 70f), 1e-4f)
        assertEquals(0f, Explosion.falloff(70f, 70f), 1e-4f)
        assertTrue(Explosion.falloff(35f, 70f) in 0.4f..0.6f)
    }
    @Test fun `explosion breaks a nearby destructible wall`() {
        val m = MapLoader.load(Stages.byId("arena1")).tileMap
        // find an internal destructible wall and explode on it with full damage
        var wx = -1; var wy = -1
        loop@ for (ty in 1 until m.height - 1) for (tx in 1 until m.width - 1) {
            if (m.tileAt(tx, ty) == Tile.WALL && m.hp[m.index(tx, ty)].isFinite()) { wx = tx; wy = ty; break@loop }
        }
        Explosion.applyWallDamage(m, wx * Tuning.TILE + 16f, wy * Tuning.TILE + 16f, PlayerConfig())
        assertEquals(Tile.FLOOR, m.tileAt(wx, wy)) // 120 dmg at center > 90 HP -> broken
    }
}
