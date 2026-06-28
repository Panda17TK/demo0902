package io.github.panda17tk.arpg.map

import io.github.panda17tk.arpg.sim.Tuning

/** Result of damaging a tile: whether it broke (and a material should be granted). */
data class TileDamage(val broke: Boolean)

object Tiles {
    /** Damage a destructible WALL. On break -> FLOOR with ∞ HP and broke=true (caller grants material + rebuilds flow). */
    fun damageTile(map: TileMap, tx: Int, ty: Int, dmg: Float): TileDamage {
        if (!map.inBounds(tx, ty)) return TileDamage(false)
        if (map.tileAt(tx, ty) != Tile.WALL) return TileDamage(false)
        val i = map.index(tx, ty)
        if (map.hp[i].isInfinite()) return TileDamage(false)
        map.hp[i] = maxOf(0f, map.hp[i] - dmg)
        if (map.hp[i] <= 0f) {
            clearWall(map, tx, ty)
            return TileDamage(true)
        }
        return TileDamage(false)
    }

    fun clearWall(map: TileMap, tx: Int, ty: Int) {
        if (!map.inBounds(tx, ty)) return
        val i = map.index(tx, ty)
        map.setTile(tx, ty, Tile.FLOOR)
        map.hp[i] = Float.POSITIVE_INFINITY
        map.maxHp[i] = Float.POSITIVE_INFINITY
    }

    /** Can a wall be placed on this tile? Floor only, not overlapping the given AABB (player). */
    fun canPlaceWall(map: TileMap, tx: Int, ty: Int, occX: Float, occY: Float, occHalf: Float): Boolean {
        if (!map.inBounds(tx, ty)) return false
        if (map.tileAt(tx, ty) != Tile.FLOOR) return false
        val cx = tx * Tuning.TILE + Tuning.TILE / 2f
        val cy = ty * Tuning.TILE + Tuning.TILE / 2f
        val half = Tuning.TILE * 0.45f
        val overlap = kotlin.math.abs(cx - occX) < (half + occHalf) && kotlin.math.abs(cy - occY) < (half + occHalf)
        return !overlap
    }

    /** Place a destructible wall (caller checked material + canPlaceWall). */
    fun placeWall(map: TileMap, tx: Int, ty: Int) {
        if (!map.inBounds(tx, ty)) return
        val i = map.index(tx, ty)
        map.setTile(tx, ty, Tile.WALL)
        map.hp[i] = Tuning.PLACED_WALL_HP
        map.maxHp[i] = Tuning.PLACED_WALL_HP
    }
}
