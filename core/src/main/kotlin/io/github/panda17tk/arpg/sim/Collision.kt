package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.map.TileMap
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

data class CollisionResult(val x: Float, val y: Float, val hitX: Boolean, val hitY: Boolean)

/**
 * Axis-separated AABB-vs-tile collision, ported from legacy systems/physics.js moveAndCollide.
 * Sweeps from start to end position on each axis to find the nearest blocking tile face.
 * Returns the resolved position + which axes were blocked.
 */
object Collision {
    /** True when the AABB overlaps any solid tile (strictly — flush contact does not count). */
    private fun overlapsSolid(map: TileMap, x: Float, y: Float, halfW: Float, halfH: Float): Boolean {
        val x0 = floor((x - halfW) / Tuning.TILE).toInt(); val x1 = floor((x + halfW) / Tuning.TILE).toInt()
        val y0 = floor((y - halfH) / Tuning.TILE).toInt(); val y1 = floor((y + halfH) / Tuning.TILE).toInt()
        for (ty in y0..y1) for (tx in x0..x1) if (map.solidAt(tx, ty)) return true
        return false
    }

    fun moveAndCollide(
        map: TileMap, x: Float, y: Float, halfW: Float, halfH: Float, dx: Float, dy: Float,
    ): CollisionResult {
        // v2.153: an entity already embedded in rock (spawned or shoved inside) must be able to
        // leave — the face guards below only stop CROSSINGS, so an embedded start pins forever.
        if (overlapsSolid(map, x, y, halfW - 0.5f, halfH - 0.5f)) {
            return CollisionResult(x + dx, y + dy, hitX = false, hitY = false)
        }
        // --- X axis ---
        var px = x + dx
        var hitX = false
        if (dx != 0f) {
            // Scan tiles swept in x (from old to new), full y extent at original y
            val x0 = floor((min(x, px) - halfW) / Tuning.TILE).toInt()
            val x1 = floor((max(x, px) + halfW) / Tuning.TILE).toInt()
            val y0 = floor((y - halfH) / Tuning.TILE).toInt()
            val y1 = floor((y + halfH) / Tuning.TILE).toInt()
            for (ty in y0..y1) for (tx in x0..x1) {
                if (!map.solidAt(tx, ty)) continue
                val left = tx * Tuning.TILE
                val right = left + Tuning.TILE
                if (dx > 0f && x + halfW <= left && px + halfW > left) {
                    px = left - halfW; hitX = true
                } else if (dx < 0f && x - halfW >= right && px - halfW < right) {
                    px = right + halfW; hitX = true
                }
            }
        }

        // --- Y axis (use resolved px) ---
        var py = y + dy
        var hitY = false
        if (dy != 0f) {
            val x0 = floor((px - halfW) / Tuning.TILE).toInt()
            val x1 = floor((px + halfW) / Tuning.TILE).toInt()
            val y0 = floor((min(y, py) - halfH) / Tuning.TILE).toInt()
            val y1 = floor((max(y, py) + halfH) / Tuning.TILE).toInt()
            for (ty in y0..y1) for (tx in x0..x1) {
                if (!map.solidAt(tx, ty)) continue
                val top = ty * Tuning.TILE
                val bottom = top + Tuning.TILE
                if (dy > 0f && y + halfH <= top && py + halfH > top) {
                    py = top - halfH; hitY = true
                } else if (dy < 0f && y - halfH >= bottom && py - halfH < bottom) {
                    py = bottom + halfH; hitY = true
                }
            }
        }

        return CollisionResult(px, py, hitX, hitY)
    }
}
