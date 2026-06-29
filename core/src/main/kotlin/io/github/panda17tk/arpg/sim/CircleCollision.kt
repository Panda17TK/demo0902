package io.github.panda17tk.arpg.sim

import kotlin.math.hypot

/** Resolved position after a planet push-out, plus whether it hit and the speed it drove inward (for crash). */
data class CircleResult(val x: Float, val y: Float, val hit: Boolean, val inwardSpeed: Float)

/** Push an AABB body (approximated as a circle of radius bodyR) out of solid planet circles. */
object CircleCollision {
    fun resolve(x: Float, y: Float, bodyR: Float, vx: Float, vy: Float, planets: List<PlanetBody>): CircleResult {
        for (p in planets) {
            val dx = x - p.cx; val dy = y - p.cy
            val d = hypot(dx, dy)
            val minDist = p.radius + bodyR
            if (d >= minDist) continue
            // Outward normal (push right when dead-centre to avoid div-by-zero).
            val nx: Float; val ny: Float
            if (d < 1e-3f) { nx = 1f; ny = 0f } else { nx = dx / d; ny = dy / d }
            val rx = p.cx + nx * minDist; val ry = p.cy + ny * minDist
            val inward = -(vx * nx + vy * ny) // velocity component driving into the planet
            return CircleResult(rx, ry, true, inward.coerceAtLeast(0f))
        }
        return CircleResult(x, y, false, 0f)
    }
}
