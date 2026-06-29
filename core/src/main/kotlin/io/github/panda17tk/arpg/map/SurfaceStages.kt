package io.github.panda17tk.arpg.map

import io.github.panda17tk.arpg.math.Rng
import kotlin.math.abs

/** Procedurally-built planet-surface arenas (Living Planets LP-E). Smaller than space; biome-flavoured walls. */
object SurfaceStages {
    private val LEGEND = mapOf('P' to LegendEntry("player"))

    fun forBiome(biome: Biome?, seed: Long): StageDef {
        val rng = Rng(seed)
        val w = 72
        val h = 46
        val g = Array(h) { CharArray(w) { '.' } }
        for (x in 0 until w) { g[0][x] = '#'; g[h - 1][x] = '#' }
        for (y in 0 until h) { g[y][0] = '#'; g[y][w - 1] = '#' }
        val pcx = w / 2
        val pcy = h / 2
        repeat((w * h) / 35) {
            val cx = 2 + rng.nextInt(w - 4)
            val cy = 2 + rng.nextInt(h - 4)
            if (abs(cx - pcx) < 5 && abs(cy - pcy) < 5) return@repeat // keep the landing area clear
            val r = rng.nextInt(2)
            for (dy in -r..r) for (dx in -r..r) {
                val nx = cx + dx
                val ny = cy + dy
                if (nx in 1 until w - 1 && ny in 1 until h - 1 && rng.nextFloat() < 0.7f) g[ny][nx] = '#'
            }
        }
        g[pcy][pcx] = 'P'
        return StageDef(
            id = "surface_" + (biome?.name?.lowercase() ?: "rock"), name = "地表",
            wallHp = 80f, rows = g.map { String(it) }, legend = LEGEND,
        )
    }
}
