package io.github.panda17tk.arpg.map

import io.github.panda17tk.arpg.planet.PlanetBiome

/** Material/biome of a block. Drives its colour and the effect of touching it.
 *  CRYSTAL / ASH (v2.39) are purely visual variety — no touch effect. */
enum class Biome { ROCK, GRASS, SNOW, MAGMA, CRYSTAL, ASH }

/**
 * Pure, deterministic block material. In space it is per **region** (9×9-tile patches): mostly rock with
 * patches of grass / snow / magma. On a planet surface it instead leans to the planet's [PlanetBiome]
 * via [surface] (finer 3-tile patches): a nature world is grass-heavy, a magma world lava-heavy, an ice
 * world snow-heavy, etc. Either way: magma burns, snow slows, grass restores.
 */
object Biomes {
    private val TABLE = arrayOf(
        Biome.ROCK, Biome.ROCK, Biome.ROCK, Biome.ROCK, Biome.ROCK, Biome.ROCK,
        Biome.GRASS, Biome.GRASS, Biome.SNOW, Biome.SNOW, Biome.MAGMA, Biome.MAGMA,
        Biome.CRYSTAL, Biome.CRYSTAL, Biome.ASH, Biome.ASH, // v2.39: crystal veins + ash fields
    )

    fun of(tx: Int, ty: Int): Biome {
        val h = Math.floorMod((tx / 9) * 374761393 xor (ty / 9) * 668265263, TABLE.size)
        return TABLE[h]
    }

    // Per-planet surface material weights, sampled at a finer 3-tile patch so a surface reads as its biome.
    private val SURFACE = mapOf(
        PlanetBiome.NATURE to arrayOf(Biome.GRASS, Biome.GRASS, Biome.GRASS, Biome.GRASS, Biome.GRASS, Biome.ROCK, Biome.GRASS, Biome.ROCK),
        PlanetBiome.MAGMA to arrayOf(Biome.MAGMA, Biome.MAGMA, Biome.MAGMA, Biome.ROCK, Biome.MAGMA, Biome.ROCK, Biome.MAGMA, Biome.ROCK),
        PlanetBiome.ICE to arrayOf(Biome.SNOW, Biome.SNOW, Biome.SNOW, Biome.SNOW, Biome.ROCK, Biome.SNOW, Biome.ROCK, Biome.SNOW),
        PlanetBiome.GAS to arrayOf(Biome.ROCK, Biome.ROCK, Biome.ROCK, Biome.GRASS, Biome.ROCK, Biome.ROCK, Biome.ROCK, Biome.GRASS),
        PlanetBiome.DEAD to arrayOf(Biome.ROCK, Biome.ASH, Biome.ROCK, Biome.ASH, Biome.CRYSTAL, Biome.ROCK), // v2.39: ash plains + rare crystal
        PlanetBiome.LONELY to arrayOf(Biome.ROCK, Biome.ROCK, Biome.CRYSTAL, Biome.ROCK, Biome.SNOW, Biome.ROCK, Biome.ROCK, Biome.GRASS), // v2.39: quiet crystal veins
    )

    /** Block material on a planet surface — biased to the planet's [PlanetBiome] (finer patches than space). */
    fun surface(planet: PlanetBiome, tx: Int, ty: Int): Biome {
        val table = SURFACE[planet] ?: return of(tx, ty)
        val h = Math.floorMod((tx / 3) * 374761393 xor (ty / 3) * 668265263, table.size)
        return table[h]
    }
}
