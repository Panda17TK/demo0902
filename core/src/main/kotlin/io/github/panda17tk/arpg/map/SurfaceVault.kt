package io.github.panda17tk.arpg.map

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * v2.95 地下遺構 — every surface hides ONE sealed vault: a square of indestructible plating
 * (DOOR tiles — guns and grenades cannot open it) with a single two-tile mouth. Inside wait
 * a keeper and its hoard. Pure map stamping; WorldFactory calls this BEFORE the flow field
 * reads the map, so pathing knows the walls from the first tick.
 */
object SurfaceVault {
    const val R = 3 // ring radius in tiles (7x7 footprint, 5x5 chamber)

    /** Stamp the vault; returns its centre in world px. Deterministic from [seed]. */
    fun place(map: TileMap, spawnX: Float, spawnY: Float, seed: Long): Pair<Float, Float> {
        val rng = Rng(seed xor 0x7A017A01L)
        val a = rng.nextFloat() * TAU
        val d = (45 + rng.nextInt(26)) * Tuning.TILE // 45–70 tiles out — a real walk from the pad
        val cx = ((spawnX + cos(a) * d) / Tuning.TILE).toInt().coerceIn(R + 2, map.width - R - 3)
        val cy = ((spawnY + sin(a) * d) / Tuning.TILE).toInt().coerceIn(R + 2, map.height - R - 3)
        val mouthSide = rng.nextInt(4)
        for (dy in -R..R) for (dx in -R..R) {
            val ring = max(abs(dx), abs(dy)) == R
            val mouth = when (mouthSide) {
                0 -> dy == R && dx in 0..1
                1 -> dy == -R && dx in 0..1
                2 -> dx == R && dy in 0..1
                else -> dx == -R && dy in 0..1
            }
            map.setTile(cx + dx, cy + dy, if (ring && !mouth) Tile.DOOR else Tile.FLOOR)
        }
        return (cx + 0.5f) * Tuning.TILE to (cy + 0.5f) * Tuning.TILE
    }

    private val TAU = (Math.PI * 2.0).toFloat()
}
