package io.github.panda17tk.arpg.map

/** Material/biome of a block. Drives its colour and the effect of touching it. */
enum class Biome { ROCK, GRASS, SNOW, MAGMA }

/**
 * Pure, deterministic biome per **region** (9×9-tile patches), so blocks come in varied terrains —
 * mostly rock, with patches of grassland / snow / magma. Magma burns, snow slows, grass restores.
 */
object Biomes {
    private val TABLE = arrayOf(
        Biome.ROCK, Biome.ROCK, Biome.ROCK, Biome.ROCK, Biome.ROCK, Biome.ROCK,
        Biome.GRASS, Biome.GRASS, Biome.SNOW, Biome.SNOW, Biome.MAGMA, Biome.MAGMA,
    )

    fun of(tx: Int, ty: Int): Biome {
        val h = Math.floorMod((tx / 9) * 374761393 xor (ty / 9) * 668265263, TABLE.size)
        return TABLE[h]
    }
}
