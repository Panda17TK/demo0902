package io.github.panda17tk.arpg.pathfinding

import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sign

/** Integer-grid line of sight (Bresenham). False if any solid tile lies on the line. */
object Los {
    fun hasLineOfSight(map: TileMap, x0: Float, y0: Float, x1: Float, y1: Float): Boolean {
        var cx = floor(x0 / Tuning.TILE).toInt()
        var cy = floor(y0 / Tuning.TILE).toInt()
        val tx = floor(x1 / Tuning.TILE).toInt()
        val ty = floor(y1 / Tuning.TILE).toInt()
        val dx = sign((tx - cx).toFloat()).toInt()
        val dy = sign((ty - cy).toFloat()).toInt()
        val nx = abs(tx - cx)
        val ny = abs(ty - cy)
        var err = nx - ny
        var guard = 0
        while (!(cx == tx && cy == ty) && guard++ < 1200) {
            if (map.solidAt(cx, cy)) return false
            val e2 = 2 * err
            if (e2 > -ny) { err -= ny; cx += dx }
            if (e2 < nx) { err += nx; cy += dy }
        }
        return true
    }
}
