package io.github.panda17tk.arpg.map

/**
 * Mutable tile grid (row-major). `hp`/`maxHp` use Float.POSITIVE_INFINITY for
 * indestructible tiles (border walls, doors). Ported from legacy state.map/tileHP.
 */
class TileMap(
    val width: Int,
    val height: Int,
    private val tiles: Array<Tile>,
    val hp: FloatArray,
    val maxHp: FloatArray,
) {
    fun inBounds(tx: Int, ty: Int): Boolean = tx in 0 until width && ty in 0 until height
    fun index(tx: Int, ty: Int): Int = ty * width + tx

    fun tileAt(tx: Int, ty: Int): Tile = if (inBounds(tx, ty)) tiles[index(tx, ty)] else Tile.WALL

    /** Out-of-bounds is treated as solid (matches legacy solidAt). */
    fun solidAt(tx: Int, ty: Int): Boolean = tileAt(tx, ty).solid

    /** A breakable wall (finite HP) — excludes the indestructible border so it doesn't form a cluster. */
    fun destructibleAt(tx: Int, ty: Int): Boolean =
        inBounds(tx, ty) && tiles[index(tx, ty)].solid && maxHp[index(tx, ty)].isFinite()

    fun setTile(tx: Int, ty: Int, t: Tile) { if (inBounds(tx, ty)) tiles[index(tx, ty)] = t }

    companion object {
        /** Build from char rows with default HP (∞ for WALL/DOOR; loader overrides WALL HP). */
        fun fromRows(rows: List<String>): TileMap {
            val h = rows.size
            val w = rows[0].length
            val tiles = Array(w * h) { Tile.FLOOR }
            val hp = FloatArray(w * h) { Float.POSITIVE_INFINITY }
            val maxHp = FloatArray(w * h) { Float.POSITIVE_INFINITY }
            for (y in 0 until h) for (x in 0 until w) tiles[y * w + x] = Tile.fromChar(rows[y][x])
            return TileMap(w, h, tiles, hp, maxHp)
        }
    }
}
