package io.github.panda17tk.arpg.sim

import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * A connected blob of destructible wall tiles. Coordinates are in whatever unit [WallGravity.detect]
 * was fed (tile space); the gravity system converts to world units before calling [WallGravity.gravityAt].
 */
data class Cluster(val cx: Float, val cy: Float, val radius: Float, val count: Int)

/**
 * Pure wall-cluster detection + gravity field (no libGDX/Fleks). A blob of connected wall tiles
 * whose radius is ≥ 2 tiles emits a weak pull toward its centroid; [gravityAt] sums the pulls of
 * nearby clusters with linear falloff. Detection runs over a bounded window so it stays O(window²)
 * on huge maps.
 */
object WallGravity {
    private val NB = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))

    private fun key(x: Int, y: Int): Long = (x.toLong() shl 32) or (y.toLong() and 0xffffffffL)

    fun detect(minTx: Int, maxTx: Int, minTy: Int, maxTy: Int, isWall: (Int, Int) -> Boolean): List<Cluster> {
        val visited = HashSet<Long>()
        val out = ArrayList<Cluster>()
        for (ty in minTy..maxTy) for (tx in minTx..maxTx) {
            if (!isWall(tx, ty)) continue
            val start = key(tx, ty)
            if (start in visited) continue
            visited.add(start)
            val stack = ArrayDeque<Long>()
            stack.addLast(start)
            val xs = ArrayList<Int>(); val ys = ArrayList<Int>()
            while (stack.isNotEmpty()) {
                val cur = stack.removeLast()
                val cx = (cur shr 32).toInt(); val cy = cur.toInt()
                xs.add(cx); ys.add(cy)
                for (n in NB) {
                    val nx = cx + n[0]; val ny = cy + n[1]
                    if (nx < minTx || nx > maxTx || ny < minTy || ny > maxTy) continue
                    if (!isWall(nx, ny)) continue
                    val nk = key(nx, ny)
                    if (nk in visited) continue
                    visited.add(nk); stack.addLast(nk)
                }
            }
            val cnt = xs.size
            var sx = 0.0; var sy = 0.0
            for (i in 0 until cnt) { sx += xs[i] + 0.5; sy += ys[i] + 0.5 }
            val ccx = (sx / cnt).toFloat(); val ccy = (sy / cnt).toFloat()
            var rad = 0f
            for (i in 0 until cnt) {
                val d = hypot((xs[i] + 0.5f) - ccx, (ys[i] + 0.5f) - ccy)
                if (d > rad) rad = d
            }
            if (rad >= 2f) out.add(Cluster(ccx, ccy, rad, cnt))
        }
        return out
    }

    /** Sum of weak pulls toward each cluster within [range], magnitude ∝ strength·(1 − d/range). */
    fun gravityAt(clusters: List<Cluster>, x: Float, y: Float, range: Float, strength: Float): Pair<Float, Float> {
        var ax = 0f; var ay = 0f
        for (c in clusters) {
            val dx = c.cx - x; val dy = c.cy - y
            val d = hypot(dx, dy)
            if (d > range || d < 1e-3f) continue
            // Bigger clusters pull harder (∝ √blocks, so it grows but stays sane on huge stars).
            val m = strength * sqrt(c.count.toFloat()) * (1f - d / range) / d
            ax += dx * m; ay += dy * m
        }
        return ax to ay
    }
}
