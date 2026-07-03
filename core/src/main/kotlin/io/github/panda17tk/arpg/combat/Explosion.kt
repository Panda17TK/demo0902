package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.config.PlayerConfig
import io.github.panda17tk.arpg.map.Tile
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.map.Tiles
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.floor
import kotlin.math.hypot

/** Explosion math + wall application (legacy projectiles.js explode). Mob/self damage handled by callers. */
object Explosion {
    /** Linear falloff: 1 at center, 0 at [radius]. */
    fun falloff(dist: Float, radius: Float): Float = (1f - dist / radius).coerceIn(0f, 1f)

    /** Damage destructible WALL tiles within explodeRadius·[radiusMul] of (x,y) with wallDmg*(1-d/r).
     *  [radiusMul] (v2.37) lets a graded grenade widen its wall crater along with the blast. */
    fun applyWallDamage(map: TileMap, x: Float, y: Float, cfg: PlayerConfig, radiusMul: Float = 1f) {
        val r = cfg.explodeRadius * radiusMul
        val tx0 = maxOf(1, floor((x - r) / Tuning.TILE).toInt())
        val ty0 = maxOf(1, floor((y - r) / Tuning.TILE).toInt())
        val tx1 = minOf(map.width - 2, floor((x + r) / Tuning.TILE).toInt())
        val ty1 = minOf(map.height - 2, floor((y + r) / Tuning.TILE).toInt())
        for (ty in ty0..ty1) for (tx in tx0..tx1) {
            if (map.tileAt(tx, ty) != Tile.WALL) continue
            val cx = tx * Tuning.TILE + Tuning.TILE / 2f
            val cy = ty * Tuning.TILE + Tuning.TILE / 2f
            val d = hypot(cx - x, cy - y)
            if (d <= r) Tiles.damageTile(map, tx, ty, cfg.explodeWallDmg * (1f - d / r))
        }
    }

    /**
     * Flatten EVERY destructible WALL whose centre lies within [radius] of (x,y) — a clean crater rather than a
     * falloff (used by the beam's impact blast). Indestructible (∞-HP) border walls are left alone. Returns true
     * if any wall broke (so the caller can rebuild the flow field).
     */
    fun blastWalls(map: TileMap, x: Float, y: Float, radius: Float): Boolean {
        var broke = false
        val tx0 = maxOf(1, floor((x - radius) / Tuning.TILE).toInt())
        val ty0 = maxOf(1, floor((y - radius) / Tuning.TILE).toInt())
        val tx1 = minOf(map.width - 2, floor((x + radius) / Tuning.TILE).toInt())
        val ty1 = minOf(map.height - 2, floor((y + radius) / Tuning.TILE).toInt())
        for (ty in ty0..ty1) for (tx in tx0..tx1) {
            if (map.tileAt(tx, ty) != Tile.WALL) continue
            val cx = tx * Tuning.TILE + Tuning.TILE / 2f
            val cy = ty * Tuning.TILE + Tuning.TILE / 2f
            if (hypot(cx - x, cy - y) <= radius && Tiles.damageTile(map, tx, ty, Float.MAX_VALUE).broke) broke = true
        }
        return broke
    }
}
