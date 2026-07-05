package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import kotlin.math.sin

/**
 * v2.74 天候 — each planet server keeps its own climate loop running, and some of them still
 * rain. Deterministic per planet (like its context, market and quest), purely cosmetic, and
 * pure math: particle positions are functions of time, so the sky needs no state at all.
 */
enum class WeatherKind { CLEAR, RAIN, SNOW, ASH, DUSTWIND }

/** How one weather draws: particle count, fall/drift in screen-fractions per second, dot size. */
data class WeatherParams(
    val count: Int,
    val fallPerSec: Float,  // vertical screen-fractions per second (downward)
    val driftPerSec: Float, // horizontal screen-fractions per second
    val size: Float,        // particle radius in dp
    val streak: Boolean,    // true = draw as a short streak along the motion (rain / dust wind)
    val sway: Float,        // sideways wobble amplitude in dp (snow drifts, ash floats)
)

object Weather {
    /** The climate a planet runs — deterministic from its id, and quiet more often than not. */
    fun kindFor(planetId: Long, biome: PlanetBiome): WeatherKind {
        val r = Rng(planetId * 53L + biome.ordinal.toLong() * 29L + SALT)
        val roll = r.nextFloat()
        return when (biome) {
            PlanetBiome.NATURE -> if (roll < 0.45f) WeatherKind.RAIN else WeatherKind.CLEAR
            PlanetBiome.GAS -> if (roll < 0.60f) WeatherKind.RAIN else WeatherKind.CLEAR
            PlanetBiome.ICE -> if (roll < 0.60f) WeatherKind.SNOW else WeatherKind.CLEAR
            PlanetBiome.MAGMA -> if (roll < 0.55f) WeatherKind.ASH else WeatherKind.CLEAR
            PlanetBiome.DEAD -> if (roll < 0.40f) WeatherKind.ASH else WeatherKind.CLEAR
            PlanetBiome.LONELY -> if (roll < 0.35f) WeatherKind.DUSTWIND else WeatherKind.CLEAR
        }
    }

    fun paramsFor(kind: WeatherKind): WeatherParams = when (kind) {
        WeatherKind.CLEAR -> WeatherParams(0, 0f, 0f, 0f, streak = false, sway = 0f)
        WeatherKind.RAIN -> WeatherParams(90, 0.9f, -0.08f, 1.1f, streak = true, sway = 0f)
        WeatherKind.SNOW -> WeatherParams(70, 0.12f, -0.02f, 1.6f, streak = false, sway = 10f)
        WeatherKind.ASH -> WeatherParams(50, 0.06f, 0.015f, 1.4f, streak = false, sway = 6f)
        WeatherKind.DUSTWIND -> WeatherParams(60, 0.02f, -0.55f, 1.0f, streak = true, sway = 3f)
    }

    /**
     * Particle [i]'s position at [t] seconds, in screen fractions (x wraps, y falls and wraps).
     * Golden-ratio hashing spreads the flakes without any per-particle state.
     */
    fun pos(i: Int, t: Float, p: WeatherParams): Pair<Float, Float> {
        val fx0 = frac(i * 0.6180340f)
        val fy0 = frac(i * 0.7548777f)
        val speedJitter = 0.75f + 0.5f * frac(i * 0.5698403f) // some fall faster than others
        val x = frac(fx0 + t * p.driftPerSec * speedJitter)
        val y = frac(fy0 - t * p.fallPerSec * speedJitter) // y-up screens: falling = decreasing
        return x to y
    }

    /** The sideways wobble of particle [i] at [t], in dp (0 when the weather has no sway). */
    fun sway(i: Int, t: Float, p: WeatherParams): Float =
        if (p.sway <= 0f) 0f else p.sway * sin(t * (0.9f + frac(i * 0.318309f)) + i * 2.4f)

    private fun frac(v: Float): Float {
        val f = v - v.toInt()
        return if (f < 0f) f + 1f else f
    }

    private const val SALT = 0x0EA7_1E55L
}
