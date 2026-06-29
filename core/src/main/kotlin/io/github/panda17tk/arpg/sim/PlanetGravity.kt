package io.github.panda17tk.arpg.sim

import kotlin.math.hypot

/** Gravity from discrete planets, plus a helper that sums planet pull with wall-cluster pull. */
object PlanetGravity {
    /** Sum of pulls toward each planet within its gravityRange; magnitude ∝ mass·(1 − d/range). */
    fun gravityAccelAt(planets: List<PlanetBody>, x: Float, y: Float): Pair<Float, Float> {
        var ax = 0f; var ay = 0f
        for (p in planets) {
            val dx = p.cx - x; val dy = p.cy - y
            val d = hypot(dx, dy)
            if (d > p.gravityRange || d < 1e-3f) continue
            val m = p.mass * (1f - d / p.gravityRange) / d
            ax += dx * m; ay += dy * m
        }
        return ax to ay
    }

    /** Planet pull + wall-cluster pull combined into one acceleration. */
    fun combinedGravityAccel(
        planets: List<PlanetBody>, clusters: List<Cluster>, x: Float, y: Float,
        clusterRange: Float, clusterStrength: Float,
    ): Pair<Float, Float> {
        val (px, py) = gravityAccelAt(planets, x, y)
        val (cx, cy) = WallGravity.gravityAt(clusters, x, y, clusterRange, clusterStrength)
        return (px + cx) to (py + cy)
    }
}
