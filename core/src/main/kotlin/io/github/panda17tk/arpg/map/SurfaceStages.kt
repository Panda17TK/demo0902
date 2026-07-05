package io.github.panda17tk.arpg.map

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import kotlin.math.abs

/**
 * Procedurally-built planet-surface arenas (Living Planets LP-E). Smaller than space; the planet's
 * [PlanetBiome] shapes the terrain — arena size, how dense the obstacles are and how big they get.
 * Block *materials* (and their touch effects: magma burns, snow slows, grass restores) still come from
 * [Biomes]; this only decides the layout's flavour.
 */
object SurfaceStages {
    private val LEGEND = mapOf('P' to LegendEntry("player"))

    /** Per-biome terrain shape: arena size, obstacle density (smaller divisor = denser) and blob radius. */
    private data class Shape(val w: Int, val h: Int, val densityDiv: Int, val maxBlob: Int)

    private fun shapeOf(biome: PlanetBiome?): Shape = when (biome) {
        PlanetBiome.NATURE -> Shape(72, 46, 30, 1) // lush: plenty of cover
        PlanetBiome.MAGMA -> Shape(72, 46, 26, 2) // jagged volcanic rock, lots of it
        PlanetBiome.ICE -> Shape(72, 46, 34, 1) // open snowfields
        PlanetBiome.GAS -> Shape(80, 50, 64, 1) // wide and open, few obstacles
        PlanetBiome.DEAD -> Shape(72, 46, 38, 2) // barren rock
        PlanetBiome.LONELY -> Shape(48, 32, 70, 1) // small and sparse
        null -> Shape(72, 46, 35, 1) // neutral fallback
    }

    fun forBiome(biome: PlanetBiome?, seed: Long): StageDef {
        val rng = Rng(seed)
        val (w, h, densityDiv, maxBlob) = shapeOf(biome)
        val g = Array(h) { CharArray(w) { '.' } }
        // v2.83 ループ地表: no border ring — the outermost row/column stays open floor and the
        // player wraps across it (MovementSystem). Obstacle blobs never write there (1 until w-1).
        val pcx = w / 2
        val pcy = h / 2
        repeat((w * h) / densityDiv) {
            val cx = 2 + rng.nextInt(w - 4)
            val cy = 2 + rng.nextInt(h - 4)
            if (abs(cx - pcx) < 5 && abs(cy - pcy) < 5) return@repeat // keep the landing area clear
            val r = rng.nextInt(maxBlob + 1)
            for (dy in -r..r) for (dx in -r..r) {
                val nx = cx + dx
                val ny = cy + dy
                if (nx in 1 until w - 1 && ny in 1 until h - 1 && rng.nextFloat() < 0.7f) g[ny][nx] = '#'
            }
        }
        g[pcy][pcx] = 'P'
        return StageDef(
            id = "surface_" + (biome?.name?.lowercase() ?: "unknown"), name = "地表",
            wallHp = 80f, rows = g.map { String(it) }, legend = LEGEND,
        )
    }
}
