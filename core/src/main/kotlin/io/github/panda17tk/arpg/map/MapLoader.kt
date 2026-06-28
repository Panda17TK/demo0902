package io.github.panda17tk.arpg.map

import io.github.panda17tk.arpg.sim.Tuning

/** Result of loading a stage: the tile map, the player spawn point, and other spawn markers. */
class LoadedMap(
    val tileMap: TileMap,
    val playerSpawnX: Float,
    val playerSpawnY: Float,
    val spawns: List<SpawnMarker>,
)

object MapLoader {
    fun load(stage: StageDef): LoadedMap {
        val map = TileMap.fromRows(stage.rows)
        val w = map.width
        val h = map.height
        var playerX = (w / 2) * Tuning.TILE + Tuning.TILE / 2f
        var playerY = (h / 2) * Tuning.TILE + Tuning.TILE / 2f
        val spawns = mutableListOf<SpawnMarker>()

        for (ty in 0 until h) for (tx in 0 until w) {
            val c = stage.rows[ty][tx]
            if (c == '#' || c == 'D') continue
            val cx = (tx + 0.5f) * Tuning.TILE
            val cy = (ty + 0.5f) * Tuning.TILE
            val entry = stage.legend[c]
            if (entry != null) {
                when (entry.kind) {
                    "player" -> { playerX = cx; playerY = cy }
                    else -> spawns.add(SpawnMarker(entry.kind, entry.name, cx, cy))
                }
            }
            // markers and unknown chars become floor (TileMap already parsed them as FLOOR)
        }

        // Wall HP: border = indestructible (∞), internal '#' = wallHp.
        for (ty in 0 until h) for (tx in 0 until w) {
            if (map.tileAt(tx, ty) == Tile.WALL) {
                val border = tx == 0 || ty == 0 || tx == w - 1 || ty == h - 1
                val v = if (border) Float.POSITIVE_INFINITY else stage.wallHp
                map.hp[map.index(tx, ty)] = v
                map.maxHp[map.index(tx, ty)] = v
            }
        }
        return LoadedMap(map, playerX, playerY, spawns)
    }
}
