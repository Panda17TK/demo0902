package io.github.panda17tk.arpg.map

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.Tuning

/**
 * v2.78 地表の装飾 — the small, harmless life of a surface: trees, grass tufts, flowers,
 * rocks, and each biome's own furniture (ice spikes, vents, cairns, drifting spores…).
 * Pure and deterministic per landing seed; the factory filters out anything under a wall,
 * and the renderer draws the survivors beneath the actors. Nothing here collides.
 */
enum class DecorKind {
    TREE,          // canopy tree (nature)
    GRASS,         // a small tuft
    FLOWER,        // petals around a heart — hue varies
    ROCK,          // a plain boulder pair
    DEAD_TREE,     // bare trunk and branches (dead / magma)
    ICE_SPIKE,     // a pale spire (ice)
    CRYSTAL_SHARD, // a glinting shard (dead / lonely / ice)
    VENT,          // a fumarole breathing faint smoke (magma)
    SPORE,         // a translucent drifting bulb (gas)
    CAIRN,         // stacked waystones (dead / lonely)
}

/** One placed decoration: kind + world position + size scale + a 0..1 hue pick for variety. */
data class Decor(val kind: DecorKind, val x: Float, val y: Float, val scale: Float, val hue: Float)

object SurfaceDecor {
    /** What grows where — weighted palettes; biome flavour without a single asset. */
    private val PALETTE = mapOf(
        PlanetBiome.NATURE to arrayOf(
            DecorKind.TREE, DecorKind.TREE, DecorKind.TREE, DecorKind.GRASS, DecorKind.GRASS,
            DecorKind.GRASS, DecorKind.FLOWER, DecorKind.FLOWER, DecorKind.ROCK,
        ),
        PlanetBiome.MAGMA to arrayOf(
            DecorKind.VENT, DecorKind.VENT, DecorKind.ROCK, DecorKind.ROCK, DecorKind.ROCK,
            DecorKind.DEAD_TREE, DecorKind.CRYSTAL_SHARD,
        ),
        PlanetBiome.ICE to arrayOf(
            DecorKind.ICE_SPIKE, DecorKind.ICE_SPIKE, DecorKind.ICE_SPIKE, DecorKind.ROCK,
            DecorKind.GRASS, DecorKind.CRYSTAL_SHARD,
        ),
        PlanetBiome.GAS to arrayOf(
            DecorKind.SPORE, DecorKind.SPORE, DecorKind.SPORE, DecorKind.GRASS,
            DecorKind.FLOWER, DecorKind.ROCK,
        ),
        PlanetBiome.DEAD to arrayOf(
            DecorKind.DEAD_TREE, DecorKind.DEAD_TREE, DecorKind.CAIRN, DecorKind.ROCK,
            DecorKind.ROCK, DecorKind.CRYSTAL_SHARD,
        ),
        PlanetBiome.LONELY to arrayOf(
            DecorKind.ROCK, DecorKind.ROCK, DecorKind.CAIRN, DecorKind.GRASS,
            DecorKind.CRYSTAL_SHARD, DecorKind.FLOWER, // one flower on a lonely rock, sometimes
        ),
    )

    /** Items per 1000 tiles — dense enough to dress the ground, sparse enough to read it. */
    private const val PER_KILOTILE = 42f

    /**
     * Scatter decorations across a surface of [worldW]×[worldH] world units. Deterministic
     * per ([biome],[seed]); keeps a clear circle around the landing pad at the arena centre.
     */
    fun scatter(biome: PlanetBiome, seed: Long, worldW: Float, worldH: Float): List<Decor> {
        val palette = PALETTE.getValue(biome)
        val rng = Rng(seed xor 0xDEC0_0001L)
        val tiles = (worldW / Tuning.TILE) * (worldH / Tuning.TILE)
        val count = (tiles / 1000f * PER_KILOTILE).toInt().coerceAtLeast(24)
        val margin = Tuning.TILE * 1.5f
        val cx = worldW / 2f; val cy = worldH / 2f
        val clearR = Tuning.TILE * 3f // the landing pad stays uncluttered
        val out = ArrayList<Decor>(count)
        repeat(count) {
            val x = margin + rng.nextFloat() * (worldW - margin * 2f)
            val y = margin + rng.nextFloat() * (worldH - margin * 2f)
            val kind = palette[rng.nextInt(palette.size)]
            val scale = 0.7f + rng.nextFloat() * 0.7f
            val hue = rng.nextFloat()
            val dx = x - cx; val dy = y - cy
            if (dx * dx + dy * dy < clearR * clearR) return@repeat
            out.add(Decor(kind, x, y, scale, hue))
        }
        return out
    }
}
