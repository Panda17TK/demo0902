package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import kotlin.math.hypot

/**
 * A discrete planet: world-space centre, solid radius, gravity strength (mass) and reach, ecological biome,
 * plus a stable [id] (deterministic from the stage seed + index) so a planet's society memory can persist
 * across landings (Living Planets: 惑星はステージではない。記憶を持つ世界である。).
 */
data class PlanetBody(
    val cx: Float,
    val cy: Float,
    val radius: Float,
    val mass: Float,
    val gravityRange: Float,
    val biome: PlanetBiome,
    val id: Long = 0L,
    val context: PlanetContext = PlanetContext.NEUTRAL,
)

/**
 * Pure, deterministic placement of discrete planets in a space stage. Planets keep a minimum distance
 * from the player spawn and never overlap each other. Foundation for the Living Planets expansion
 * (each body can later carry surface/biome data for landing).
 */
object Planets {
    fun place(
        worldW: Float, worldH: Float, playerX: Float, playerY: Float, count: Int, rng: Rng,
        minRadius: Float = 64f, maxRadius: Float = 160f, margin: Float = 96f, minPlayerDist: Float = 320f,
        seed: Long = 0L,
    ): List<PlanetBody> {
        val out = ArrayList<PlanetBody>()
        val biomes = PlanetBiome.values()
        var attempts = 0
        while (out.size < count && attempts < count * 40) {
            attempts++
            val radius = rng.range(minRadius, maxRadius)
            val cx = rng.range(radius + margin, worldW - radius - margin)
            val cy = rng.range(radius + margin, worldH - radius - margin)
            if (hypot(cx - playerX, cy - playerY) < minPlayerDist + radius) continue
            if (out.any { hypot(cx - it.cx, cy - it.cy) < radius + it.radius + margin }) continue
            val biome = biomes[rng.nextInt(biomes.size)]
            val id = idFor(seed, out.size)
            out.add(PlanetBody(cx, cy, radius, radius * MASS_PER_RADIUS, radius + GRAVITY_REACH, biome, id, PlanetContext.contextFor(id, biome)))
        }
        return out
    }

    /** Stable, deterministic id for the [index]-th planet placed in a stage of the given [seed] (golden-ratio mix). */
    fun idFor(seed: Long, index: Int): Long = seed xor (index.toLong() * GOLDEN)

    private const val GOLDEN: Long = -0x61c8864680b583ebL // 0x9E3779B97F4A7C15 as a signed Long (Fibonacci hashing)
    private const val MASS_PER_RADIUS = 1.2f // gravity "mass" scales with size
    private const val GRAVITY_REACH = 384f // gravity reaches this far beyond the surface
}
