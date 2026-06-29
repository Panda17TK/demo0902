package io.github.panda17tk.arpg.sim

import kotlin.math.hypot

/** Pure landing detection: the nearest planet whose surface the player is hovering over. */
object Landing {
    fun nearestLandable(px: Float, py: Float, planets: List<PlanetBody>, range: Float): PlanetBody? {
        var best: PlanetBody? = null
        var bestD = Float.MAX_VALUE
        for (p in planets) {
            val d = hypot(p.cx - px, p.cy - py)
            if (d <= p.radius + range && d < bestD) { bestD = d; best = p }
        }
        return best
    }
}
