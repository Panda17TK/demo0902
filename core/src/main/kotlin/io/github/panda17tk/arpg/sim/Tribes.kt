package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng

/**
 * Per-run enemy tribes (pure). Each mob belongs to the tribe of its **nearest centre** (so
 * spatially-close spawns herd together), a symmetric **hostility matrix** decides which tribe
 * pairs fight on sight, and each tribe has an **intelligence** (0..1) feeding tactical AI.
 */
class Tribes(
    val centers: List<FloatArray>,
    private val hostile: Array<BooleanArray>,
    private val intel: FloatArray = FloatArray(centers.size),
) {
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

    fun areHostile(a: Int, b: Int): Boolean = when {
        a == b -> false
        a == ROGUE || b == ROGUE -> true // v2.83: the rogue drifter answers to no banner
        else -> a in hostile.indices && b in hostile.indices && hostile[a][b]
    }

    fun intelligenceOf(tribe: Int): Float = if (tribe in intel.indices) intel[tribe] else 0f

    companion object {
        /** v2.83: the bannerless tribe — hostile to every other tribe (and only ever spawned, never built). */
        const val ROGUE = -9

        fun build(numTribes: Int, worldW: Float, worldH: Float, hostileChance: Float, rng: Rng): Tribes {
            val n = numTribes.coerceAtLeast(1)
            val centers = (0 until n).map { floatArrayOf(rng.nextFloat() * worldW, rng.nextFloat() * worldH) }
            val hostile = Array(n) { BooleanArray(n) }
            for (a in 0 until n) for (b in a + 1 until n) {
                val h = rng.nextFloat() < hostileChance
                hostile[a][b] = h; hostile[b][a] = h
            }
            val intel = FloatArray(n) { rng.nextFloat() }
            return Tribes(centers, hostile, intel)
        }
    }
}
