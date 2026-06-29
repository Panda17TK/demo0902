package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.map.Biome
import io.github.panda17tk.arpg.math.Rng
import kotlin.math.hypot

/** A discrete planet: world-space centre, solid radius, gravity strength (mass) and reach, biome tint. */
data class PlanetBody(
    val cx: Float,
    val cy: Float,
    val radius: Float,
    val mass: Float,
    val gravityRange: Float,
    val biome: Biome,
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
    ): List<PlanetBody> {
        val out = ArrayList<PlanetBody>()
        val biomes = Biome.values()
        var attempts = 0
        while (out.size < count && attempts < count * 40) {
            attempts++
            val radius = rng.range(minRadius, maxRadius)
            val cx = rng.range(radius + margin, worldW - radius - margin)
            val cy = rng.range(radius + margin, worldH - radius - margin)
            if (hypot(cx - playerX, cy - playerY) < minPlayerDist + radius) continue
            if (out.any { hypot(cx - it.cx, cy - it.cy) < radius + it.radius + margin }) continue
            out.add(PlanetBody(cx, cy, radius, radius * MASS_PER_RADIUS, radius + GRAVITY_REACH, biomes[rng.nextInt(biomes.size)]))
        }
        return out
    }

    private const val MASS_PER_RADIUS = 1.2f // gravity "mass" scales with size
    private const val GRAVITY_REACH = 384f // gravity reaches this far beyond the surface
}
