package io.github.panda17tk.arpg.audio

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.WorldMode
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * v2.63 生成オーディオ — the ambient score. Everything here is pure math: which track a scene
 * wants, what that track sounds like (a quiet pad of 2–3 partials breathing over filtered
 * noise), and the PCM render of one seamless loop. No binary assets, no Gdx, fully testable.
 *
 * Seamlessness contract: every oscillator and LFO frequency is quantized to a whole number of
 * cycles per loop, and the noise bed is head-crossfaded, so sample[n-1] → sample[0] is an
 * ordinary consecutive step — the loop point is inaudible.
 */
enum class AmbientTrack {
    TITLE,    // the front door: a calm open fifth, almost still
    SPACE,    // deep drone — the dark between memory stars
    TRAINING, // 旧式戦闘訓練: quartal, mechanical, a little rougher
    NATURE, MAGMA, ICE, GAS, DEAD, LONELY, // one voice per planet server
}

/** v2.67 状況反応: reactive layers riding on top of the base pad, faded by gameplay.
 *  PULSE = the keeper's own alarms under combat; SHIMMER = the memory core, close. */
enum class AmbientLayer { PULSE, SHIMMER }

/** The parameters of one ambient loop — the pad chord, its breathing, and the air around it. */
data class AmbientScore(
    val baseHz: Float,      // root of the pad
    val semis: List<Int>,   // partials as semitone offsets from the root
    val padAmp: Float,      // pad level (headroom-safe with noiseAmp)
    val noiseAmp: Float,    // filtered-noise (wind / hum) level
    val noiseSmooth: Float, // one-pole coefficient 0..1 — higher = darker rumble
    val lfoHz: Float,       // how fast the pad breathes
)

object AmbienceScore {
    const val RATE = 22050
    const val LOOP_SECONDS = 8
    const val SAMPLES = RATE * LOOP_SECONDS
    private const val FADE = RATE / 4 // 0.25s noise head-crossfade

    /** Which track a scene asks for. Training overrides everything (the sim has its own hum). */
    fun trackFor(mode: WorldMode?, biome: PlanetBiome?, training: Boolean): AmbientTrack = when {
        training -> AmbientTrack.TRAINING
        mode == WorldMode.SURFACE && biome != null -> when (biome) {
            PlanetBiome.NATURE -> AmbientTrack.NATURE
            PlanetBiome.MAGMA -> AmbientTrack.MAGMA
            PlanetBiome.ICE -> AmbientTrack.ICE
            PlanetBiome.GAS -> AmbientTrack.GAS
            PlanetBiome.DEAD -> AmbientTrack.DEAD
            PlanetBiome.LONELY -> AmbientTrack.LONELY
        }
        else -> AmbientTrack.SPACE
    }

    /** Each place hums differently — but all of it stays quiet (falls in line with the calm tone). */
    fun scoreFor(track: AmbientTrack): AmbientScore = when (track) {
        AmbientTrack.TITLE -> AmbientScore(110f, listOf(0, 7, 12), 0.30f, 0.04f, 0.010f, 0.10f)
        AmbientTrack.SPACE -> AmbientScore(55f, listOf(0, 12, 19), 0.32f, 0.06f, 0.006f, 0.06f)
        AmbientTrack.TRAINING -> AmbientScore(98f, listOf(0, 5, 10), 0.28f, 0.10f, 0.030f, 0.24f)
        AmbientTrack.NATURE -> AmbientScore(131f, listOf(0, 4, 9), 0.26f, 0.08f, 0.050f, 0.12f)
        AmbientTrack.MAGMA -> AmbientScore(65f, listOf(0, 3, 10), 0.28f, 0.14f, 0.004f, 0.16f)
        AmbientTrack.ICE -> AmbientScore(220f, listOf(0, 7, 14), 0.22f, 0.05f, 0.080f, 0.08f)
        AmbientTrack.GAS -> AmbientScore(82f, listOf(0, 7, 17), 0.24f, 0.12f, 0.015f, 0.10f)
        AmbientTrack.DEAD -> AmbientScore(62f, listOf(0, 6, 12), 0.26f, 0.07f, 0.008f, 0.05f)
        AmbientTrack.LONELY -> AmbientScore(147f, listOf(0, 12), 0.24f, 0.03f, 0.020f, 0.07f)
    }

    /** v2.67: how strongly the core shimmer should sing at [dist] world units from the core. */
    fun shimmerFor(dist: Float, tile: Float): Float = (1f - dist / (tile * 6f)).coerceIn(0f, 1f)

    /**
     * v2.67: render one reactive layer for [track] — same loop length and the same seamless
     * contract as the base pad (whole cycles per loop), so the three loops ride together.
     */
    fun renderLayer(layer: AmbientLayer, track: AmbientTrack): ShortArray {
        val s = scoreFor(track)
        val n = SAMPLES
        val out = ShortArray(n)
        val two_pi = (2.0 * PI)
        fun quant(hz: Float) = (hz * LOOP_SECONDS).roundToInt().coerceAtLeast(1) / LOOP_SECONDS.toFloat()
        when (layer) {
            AmbientLayer.PULSE -> {
                // A low heartbeat under combat: a sub-octave sine gated ~1.5x per second,
                // cubed so the thump has an edge but no click (the gate never jumps).
                val sub = quant(maxOf(40f, s.baseHz * 0.5f))
                val gateHz = quant(1.5f)
                for (i in 0 until n) {
                    val t = i.toDouble() / RATE
                    val gate = 0.5f + 0.5f * sin(two_pi * gateHz * t).toFloat()
                    val v = 0.30f * gate * gate * gate * sin(two_pi * sub * t).toFloat() * 32767f
                    out[i] = v.toInt().coerceIn(-32768, 32767).toShort()
                }
            }
            AmbientLayer.SHIMMER -> {
                // The memory core, close: the pad's root two octaves up + the fifth above it,
                // trembling slowly — the chord changes colour without changing key.
                val hi = quant(s.baseHz * 4f)
                val fifth = quant(s.baseHz * 6f)
                val tremHz = quant(0.75f)
                for (i in 0 until n) {
                    val t = i.toDouble() / RATE
                    val trem = 0.65f + 0.35f * sin(two_pi * tremHz * t).toFloat()
                    val v = 0.15f * trem *
                        (sin(two_pi * hi * t).toFloat() + 0.6f * sin(two_pi * fifth * t).toFloat()) * 32767f
                    out[i] = v.toInt().coerceIn(-32768, 32767).toShort()
                }
            }
        }
        return out
    }

    /**
     * v2.76 天候アンビエント: one seamless loop of a sky's sound — rain hiss, or wind in three
     * tempers. Same contract as everything here: quantized LFOs, head-crossfaded noise, pure.
     * CLEAR has no sound and returns null.
     */
    fun renderWeather(kind: io.github.panda17tk.arpg.sim.WeatherKind): ShortArray? {
        // (smooth, amp, lfoHz, lfoDepth) — rain is a steady bright hiss; the winds swell in gusts.
        val (smooth, amp, lfoHz, lfoDepth) = when (kind) {
            io.github.panda17tk.arpg.sim.WeatherKind.CLEAR -> return null
            io.github.panda17tk.arpg.sim.WeatherKind.RAIN -> listOf(0.45f, 0.16f, 0.25f, 0.15f)
            io.github.panda17tk.arpg.sim.WeatherKind.SNOW -> listOf(0.12f, 0.06f, 0.20f, 0.40f)
            io.github.panda17tk.arpg.sim.WeatherKind.ASH -> listOf(0.05f, 0.07f, 0.15f, 0.35f)
            io.github.panda17tk.arpg.sim.WeatherKind.DUSTWIND -> listOf(0.08f, 0.13f, 0.50f, 0.60f)
        }
        val n = SAMPLES
        val out = ShortArray(n)
        val rng = Rng(0x5EA7_0000L + kind.ordinal)
        val raw = FloatArray(n + FADE)
        var y = 0f
        for (i in raw.indices) {
            val x = rng.nextFloat() * 2f - 1f
            y += smooth * (x - y)
            raw[i] = y
        }
        var peak = 1e-6f
        for (v in raw) { val a = if (v < 0f) -v else v; if (a > peak) peak = a }
        val gain = amp / peak
        val lfoCyc = (lfoHz * LOOP_SECONDS).roundToInt().coerceAtLeast(1).toFloat()
        val two_pi = (2.0 * PI)
        for (i in 0 until n) {
            val noise = if (i < FADE) { // head-crossfade: the loop wrap is an ordinary step
                val w = i.toFloat() / FADE
                raw[n + i] * (1f - w) + raw[i] * w
            } else raw[i]
            val tSec = i.toDouble() / RATE
            val gust = 1f - lfoDepth * (0.5f + 0.5f * sin(two_pi * (lfoCyc / LOOP_SECONDS) * tSec).toFloat())
            val v = noise * gain * gust * 32767f
            out[i] = v.toInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    /** Render one seamless loop of [track] as 16-bit mono PCM. Deterministic per track. */
    fun render(track: AmbientTrack): ShortArray {
        val s = scoreFor(track)
        val n = SAMPLES
        val out = ShortArray(n)

        // The noise bed: one-pole-smoothed white noise, generated FADE samples long so the head
        // can crossfade over the extra tail (see the seam check in AmbienceScoreTest).
        val rng = Rng(0x50AD_0000L + track.ordinal)
        val raw = FloatArray(n + FADE)
        var y = 0f
        for (i in raw.indices) {
            val x = rng.nextFloat() * 2f - 1f
            y += s.noiseSmooth * (x - y)
            raw[i] = y
        }
        // normalize the smoothed bed to its own peak so noiseAmp means the same at every smooth
        var peak = 1e-6f
        for (v in raw) { val a = if (v < 0f) -v else v; if (a > peak) peak = a }
        val noiseGain = s.noiseAmp / peak
        val noise = FloatArray(n)
        for (i in 0 until n) noise[i] = raw[i] * noiseGain
        for (i in 0 until FADE) { // head-crossfade: out[n-1]=raw[n-1] flows into out[0]≈raw[n]
            val w = i.toFloat() / FADE
            noise[i] = (raw[n + i] * (1f - w) + raw[i] * w) * noiseGain
        }

        // The pad: partials quantized to whole cycles per loop (micro-detune falls out for free),
        // each breathing on its own phase so the chord swells like slow airflow, not a siren.
        val parts = s.semis.size
        val freqs = FloatArray(parts)
        val lfoCyc = (s.lfoHz * LOOP_SECONDS).roundToInt().coerceAtLeast(1).toFloat()
        for (k in 0 until parts) {
            val hz = s.baseHz * 2f.pow(s.semis[k] / 12f)
            freqs[k] = (hz * LOOP_SECONDS).roundToInt().coerceAtLeast(1) / LOOP_SECONDS.toFloat()
        }
        val two_pi = (2.0 * PI)
        for (i in 0 until n) {
            val t = i.toDouble() / RATE
            var pad = 0f
            for (k in 0 until parts) {
                val breathe = 0.6f + 0.4f * sin(two_pi * (lfoCyc / LOOP_SECONDS) * t + k * 2.1).toFloat()
                val amp = 1f / (1f + k * 0.5f) // higher partials sit further back
                pad += amp * breathe * sin(two_pi * freqs[k] * t).toFloat()
            }
            val v = (s.padAmp * pad / parts + noise[i]) * 32767f
            out[i] = v.toInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }
}
