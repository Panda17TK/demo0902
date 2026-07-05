package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import kotlin.math.sin

/**
 * v2.74 天候 — each planet server keeps its own climate loop running, and some of them still
 * rain. Deterministic per planet (like its context, market and quest), purely cosmetic, and
 * pure math: particle positions are functions of time, so the sky needs no state at all.
 */
enum class WeatherKind { CLEAR, RAIN, SNOW, ASH, DUSTWIND, THUNDER, FOG, AURORA }

/** How one weather draws: particle count, fall/drift in screen-fractions per second, dot size. */
data class WeatherParams(
    val count: Int,
    val fallPerSec: Float,  // vertical screen-fractions per second (downward)
    val driftPerSec: Float, // horizontal screen-fractions per second
    val size: Float,        // particle radius in dp
    val streak: Boolean,    // true = draw as a short streak along the motion (rain / dust wind)
    val sway: Float,        // sideways wobble amplitude in dp (snow drifts, ash floats)
)

/** v2.75 天候×生態系: how a sky reshapes the food web (multipliers on wildlife counts). */
data class EcologyTweaks(val predatorMul: Float, val grazerMul: Float)

object Weather {
    /** v2.75: hunters wait out the rain; herds huddle from snow and ash; dust sends everyone in.
     *  v2.77: thunder empties the trails almost completely; fog thins the herds; auroras change nothing. */
    fun ecologyTweaks(kind: WeatherKind): EcologyTweaks = when (kind) {
        WeatherKind.CLEAR -> EcologyTweaks(1f, 1f)
        WeatherKind.RAIN -> EcologyTweaks(0.5f, 1f)
        WeatherKind.SNOW -> EcologyTweaks(1f, 0.7f)
        WeatherKind.ASH -> EcologyTweaks(1f, 0.6f)
        WeatherKind.DUSTWIND -> EcologyTweaks(0.7f, 0.8f)
        WeatherKind.THUNDER -> EcologyTweaks(0.4f, 0.8f)
        WeatherKind.FOG -> EcologyTweaks(1f, 0.9f)
        WeatherKind.AURORA -> EcologyTweaks(1f, 1f)
    }

    /** The climate a planet runs — deterministic from its id, and quiet more often than not. */
    fun kindFor(planetId: Long, biome: PlanetBiome): WeatherKind {
        val r = Rng(planetId * 53L + biome.ordinal.toLong() * 29L + SALT)
        val roll = r.nextFloat()
        return when (biome) { // v2.77: the heavier/stranger skies carve their share from the old bands
            PlanetBiome.NATURE -> when { roll < 0.30f -> WeatherKind.RAIN; roll < 0.45f -> WeatherKind.THUNDER; else -> WeatherKind.CLEAR }
            PlanetBiome.GAS -> when { roll < 0.35f -> WeatherKind.RAIN; roll < 0.60f -> WeatherKind.THUNDER; else -> WeatherKind.CLEAR }
            PlanetBiome.ICE -> when { roll < 0.45f -> WeatherKind.SNOW; roll < 0.60f -> WeatherKind.AURORA; else -> WeatherKind.CLEAR }
            PlanetBiome.MAGMA -> if (roll < 0.55f) WeatherKind.ASH else WeatherKind.CLEAR
            PlanetBiome.DEAD -> when { roll < 0.25f -> WeatherKind.ASH; roll < 0.40f -> WeatherKind.FOG; else -> WeatherKind.CLEAR }
            PlanetBiome.LONELY -> if (roll < 0.35f) WeatherKind.DUSTWIND else WeatherKind.CLEAR
        }
    }

    fun paramsFor(kind: WeatherKind): WeatherParams = when (kind) {
        WeatherKind.CLEAR -> WeatherParams(0, 0f, 0f, 0f, streak = false, sway = 0f)
        WeatherKind.RAIN -> WeatherParams(90, 0.9f, -0.08f, 1.1f, streak = true, sway = 0f)
        WeatherKind.SNOW -> WeatherParams(70, 0.12f, -0.02f, 1.6f, streak = false, sway = 10f)
        WeatherKind.ASH -> WeatherParams(50, 0.06f, 0.015f, 1.4f, streak = false, sway = 6f)
        WeatherKind.DUSTWIND -> WeatherParams(60, 0.02f, -0.55f, 1.0f, streak = true, sway = 3f)
        WeatherKind.THUNDER -> WeatherParams(120, 1.05f, -0.12f, 1.1f, streak = true, sway = 0f) // a harder rain
        WeatherKind.FOG -> WeatherParams(0, 0f, 0f, 0f, streak = false, sway = 0f)    // banks, not particles
        WeatherKind.AURORA -> WeatherParams(0, 0f, 0f, 0f, streak = false, sway = 0f) // bands, not particles
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

    /**
     * v2.77 雷: the flash alpha at [t] seconds — a deterministic strike schedule (some windows
     * stay dark), each strike a sharp double-pop decaying over ~0.35s. Pure, like the rest.
     */
    fun lightningAt(t: Float): Float {
        if (t < 0f) return 0f
        val cycle = (t / LIGHTNING_CYCLE).toInt()
        val r = Rng(cycle * 613L + 97L)
        if (r.nextFloat() < 0.30f) return 0f // a quiet window — storms breathe
        val start = r.nextFloat() * (LIGHTNING_CYCLE - 0.5f)
        val local = t - cycle * LIGHTNING_CYCLE - start
        if (local < 0f || local > 0.35f) return 0f
        val d = 1f - local / 0.35f
        val flicker = if (local in 0.10f..0.16f) 0.35f else 1f // the second pop dips between strokes
        return d * d * flicker
    }

    private fun frac(v: Float): Float {
        val f = v - v.toInt()
        return if (f < 0f) f + 1f else f
    }

    private const val SALT = 0x0EA7_1E55L
    const val LIGHTNING_CYCLE = 7f // seconds per strike window (v2.77)
}
