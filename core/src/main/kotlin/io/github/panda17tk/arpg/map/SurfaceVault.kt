package io.github.panda17tk.arpg.map

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * v2.95 地下遺構 — every surface hides ONE sealed vault: indestructible plating (DOOR tiles —
 * guns and grenades cannot open it) around a chamber, a keeper and its hoard inside.
 * v2.109 深掘り: the vault now takes one of three FORMS, deterministic from the seed:
 *   0 単室 — the classic 7×7 square, one two-tile mouth.
 *   1 二重輪 — the same square plus an inner 3×3 ring whose one-tile mouth opens the OTHER
 *     way, so reaching the heart means walking the corridor around it.
 *   2 大房 — a 9×9 hall with TWO mouths on opposite sides (and, per WorldFactory, a second
 *     guard and a fatter hoard).
 * Pure map stamping; WorldFactory calls this BEFORE the flow field reads the map.
 */
object SurfaceVault {
    const val R = 3 // the classic ring radius (forms 0/1); form 2 uses [radiusFor]
    const val FORMS = 3

    /** Which form this surface's vault takes — pure in the seed (WorldFactory + tests share it). */
    fun formFor(seed: Long): Int = Rng(seed xor 0x7A02F02FL).nextInt(FORMS)

    /** The outer ring radius of [form]. */
    fun radiusFor(form: Int): Int = if (form == 2) 4 else R

    /** How many OPEN tiles the outer ring carries (its mouths). */
    fun mouthTiles(form: Int): Int = if (form == 2) 4 else 2

    /** Stamp the vault; returns its centre in world px. Deterministic from [seed]. */
    fun place(map: TileMap, spawnX: Float, spawnY: Float, seed: Long): Pair<Float, Float> {
        val rng = Rng(seed xor 0x7A017A01L)
        val form = formFor(seed)
        val r = radiusFor(form)
        val a = rng.nextFloat() * TAU
        val d = (45 + rng.nextInt(26)) * Tuning.TILE // 45–70 tiles out — a real walk from the pad
        val cx = ((spawnX + cos(a) * d) / Tuning.TILE).toInt().coerceIn(r + 2, map.width - r - 3)
        val cy = ((spawnY + sin(a) * d) / Tuning.TILE).toInt().coerceIn(r + 2, map.height - r - 3)
        val mouthSide = rng.nextInt(4)
        for (dy in -r..r) for (dx in -r..r) {
            val ring = max(abs(dx), abs(dy)) == r
            val mouth = onMouth(dx, dy, r, mouthSide) ||
                (form == 2 && onMouth(dx, dy, r, opposite(mouthSide))) // 大房: a second way in
            map.setTile(cx + dx, cy + dy, if (ring && !mouth) Tile.DOOR else Tile.FLOOR)
        }
        if (form == 1) { // 二重輪: the inner ring's one-tile mouth faces AWAY from the outer one
            val innerSide = opposite(mouthSide)
            for (dy in -1..1) for (dx in -1..1) {
                val ring = max(abs(dx), abs(dy)) == 1
                val mouth = when (innerSide) {
                    0 -> dy == 1 && dx == 0
                    1 -> dy == -1 && dx == 0
                    2 -> dx == 1 && dy == 0
                    else -> dx == -1 && dy == 0
                }
                if (ring && !mouth) map.setTile(cx + dx, cy + dy, Tile.DOOR)
            }
        }
        return (cx + 0.5f) * Tuning.TILE to (cy + 0.5f) * Tuning.TILE
    }

    private fun onMouth(dx: Int, dy: Int, r: Int, side: Int): Boolean = when (side) {
        0 -> dy == r && dx in 0..1
        1 -> dy == -r && dx in 0..1
        2 -> dx == r && dy in 0..1
        else -> dx == -r && dy in 0..1
    }

    private fun opposite(side: Int): Int = when (side) { 0 -> 1; 1 -> 0; 2 -> 3; else -> 2 }

    private val TAU = (Math.PI * 2.0).toFloat()
}
