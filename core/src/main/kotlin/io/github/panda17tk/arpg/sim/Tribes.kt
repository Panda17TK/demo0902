package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng

/**
 * Per-run enemy tribes (pure). Each mob belongs to the tribe of its **nearest centre** (so
 * spatially-close spawns herd together), and a symmetric **hostility matrix** decides which tribe
 * pairs fight on sight. Built once per run from the deterministic [Rng].
 */
class Tribes(val centers: List<FloatArray>, private val hostile: Array<BooleanArray>) {
    val count: Int get() = centers.size

    fun tribeOf(x: Float, y: Float): Int {
        var best = 0; var bestD = Float.MAX_VALUE
        for (i in centers.indices) {
            val c = centers[i]
            val dx = c[0] - x; val dy = c[1] - y
            val d = dx * dx + dy * dy
            if (d < bestD) { bestD = d; best = i }
        }
        return best
    }

    fun areHostile(a: Int, b: Int): Boolean =
        a != b && a in hostile.indices && b in hostile.indices && hostile[a][b]

    companion object {
        fun build(numTribes: Int, worldW: Float, worldH: Float, hostileChance: Float, rng: Rng): Tribes {
            val n = numTribes.coerceAtLeast(1)
            val centers = (0 until n).map { floatArrayOf(rng.nextFloat() * worldW, rng.nextFloat() * worldH) }
            val hostile = Array(n) { BooleanArray(n) }
            for (a in 0 until n) for (b in a + 1 until n) {
                val h = rng.nextFloat() < hostileChance
                hostile[a][b] = h; hostile[b][a] = h
            }
            return Tribes(centers, hostile)
        }
    }
}
