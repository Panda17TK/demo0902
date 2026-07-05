package io.github.panda17tk.arpg.audio

import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.WorldMode
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

/** v2.63 生成オーディオ: track mapping, score sanity, and the render invariants
 *  (deterministic, audible, headroom-safe, seamless at the loop point). */
class AmbienceScoreTest {
    @Test fun `training overrides every scene`() {
        assertEquals(AmbientTrack.TRAINING, AmbienceScore.trackFor(WorldMode.SPACE, null, training = true))
        assertEquals(AmbientTrack.TRAINING, AmbienceScore.trackFor(WorldMode.SURFACE, PlanetBiome.MAGMA, training = true))
    }

    @Test fun `each biome hums its own track and space is the fallback`() {
        val seen = mutableSetOf<AmbientTrack>()
        for (b in PlanetBiome.entries) {
            seen.add(AmbienceScore.trackFor(WorldMode.SURFACE, b, training = false))
        }
        assertEquals(PlanetBiome.entries.size, seen.size, "biome tracks must be distinct")
        assertTrue(AmbientTrack.SPACE !in seen && AmbientTrack.TITLE !in seen && AmbientTrack.TRAINING !in seen)
        assertEquals(AmbientTrack.SPACE, AmbienceScore.trackFor(WorldMode.SPACE, null, training = false))
        assertEquals(AmbientTrack.SPACE, AmbienceScore.trackFor(WorldMode.SURFACE, null, training = false))
        assertEquals(AmbientTrack.SPACE, AmbienceScore.trackFor(null, null, training = false))
    }

    @Test fun `every track has a distinct, headroom-safe score`() {
        val scores = AmbientTrack.entries.map { AmbienceScore.scoreFor(it) }
        assertEquals(scores.size, scores.toSet().size, "no two places may sound identical")
        for (s in scores) {
            assertTrue(s.baseHz in 40f..400f, "pad root stays in the calm register")
            assertTrue(s.semis.isNotEmpty() && s.semis.first() == 0)
            assertTrue(s.padAmp > 0f && s.noiseAmp >= 0f)
            assertTrue(s.padAmp + s.noiseAmp <= 0.8f, "headroom: never near clipping")
            assertTrue(s.noiseSmooth in 0f..1f && s.lfoHz > 0f)
        }
    }

    @Test fun `the render is deterministic and exactly one loop long`() {
        val a = AmbienceScore.render(AmbientTrack.SPACE)
        val b = AmbienceScore.render(AmbientTrack.SPACE)
        assertEquals(AmbienceScore.SAMPLES, a.size)
        assertArrayEquals(a, b)
    }

    // ── v2.67 状況反応レイヤ ─────────────────────────────────────────

    @Test fun `reactive layers share the loop contract — length, determinism, seam`() {
        for (layer in AmbientLayer.entries) {
            val a = AmbienceScore.renderLayer(layer, AmbientTrack.SPACE)
            val b = AmbienceScore.renderLayer(layer, AmbientTrack.SPACE)
            assertEquals(AmbienceScore.SAMPLES, a.size)
            assertArrayEquals(a, b)
            var peak = 0
            for (v in a) { val x = abs(v.toInt()); if (x > peak) peak = x }
            assertTrue(peak > 800, "$layer must be audible (peak=$peak)")
            assertTrue(peak < 29000, "$layer must keep headroom (peak=$peak)")
            val seam = abs(a[a.size - 1] - a[0])
            assertTrue(seam < 3000, "$layer loop seam pops (delta=$seam)")
        }
    }

    @Test fun `the two layers are different voices`() {
        val p = AmbienceScore.renderLayer(AmbientLayer.PULSE, AmbientTrack.NATURE)
        val s = AmbienceScore.renderLayer(AmbientLayer.SHIMMER, AmbientTrack.NATURE)
        var diff = 0L
        for (i in p.indices) if (p[i] != s[i]) diff++
        assertTrue(diff > p.size / 2, "pulse and shimmer must not collapse into one sound")
    }

    @Test fun `the shimmer swells only beside the memory core`() {
        val tile = 32f
        assertEquals(1f, AmbienceScore.shimmerFor(0f, tile))
        assertEquals(0f, AmbienceScore.shimmerFor(tile * 6f, tile))
        assertEquals(0f, AmbienceScore.shimmerFor(tile * 60f, tile))
        val mid = AmbienceScore.shimmerFor(tile * 3f, tile)
        assertTrue(mid > 0.4f && mid < 0.6f, "half distance ≈ half voice (was $mid)")
    }

    @Test fun `combat heat rises with violence and cools with quiet`() {
        val heat = CombatHeat()
        assertEquals(0f, heat.value)
        heat.onKill()
        assertTrue(heat.value in 0.39f..0.41f)
        heat.onPlayerHit(); heat.onPlayerHit()
        assertEquals(1f, heat.value, 0.001f) // clamped at full alarm
        heat.tick(2f)
        assertEquals(1f - 2f * CombatHeat.DECAY, heat.value, 0.001f)
        heat.tick(100f)
        assertEquals(0f, heat.value) // never below silence
    }

    @Test fun `the sky's loops honour the same seamless contract`() {
        // v2.76 天候アンビエント: rain and the three winds — pure, bounded, loop-clean.
        assertEquals(null, AmbienceScore.renderWeather(io.github.panda17tk.arpg.sim.WeatherKind.CLEAR))
        for (kind in io.github.panda17tk.arpg.sim.WeatherKind.entries) {
            val a = AmbienceScore.renderWeather(kind) ?: continue
            val b = AmbienceScore.renderWeather(kind)!!
            assertEquals(AmbienceScore.SAMPLES, a.size)
            assertArrayEquals(a, b)
            var peak = 0
            for (v in a) { val x = abs(v.toInt()); if (x > peak) peak = x }
            assertTrue(peak > 500, "$kind must be audible (peak=$peak)")
            assertTrue(peak < 29000, "$kind must keep headroom (peak=$peak)")
            val seam = abs(a[a.size - 1] - a[0])
            assertTrue(seam < 3000, "$kind loop seam pops (delta=$seam)")
        }
        // rain and dust wind are different tempers, not one file twice
        val rain = AmbienceScore.renderWeather(io.github.panda17tk.arpg.sim.WeatherKind.RAIN)!!
        val dust = AmbienceScore.renderWeather(io.github.panda17tk.arpg.sim.WeatherKind.DUSTWIND)!!
        var diff = 0L
        for (i in rain.indices) if (rain[i] != dust[i]) diff++
        assertTrue(diff > rain.size / 2)
    }

    @Test fun `every track is audible, unclipped and seamless at the loop point`() {
        for (track in AmbientTrack.entries) {
            val s = AmbienceScore.render(track)
            var peak = 0
            for (v in s) { val x = abs(v.toInt()); if (x > peak) peak = x }
            assertTrue(peak > 800, "$track must be audible (peak=$peak)")
            assertTrue(peak < 29000, "$track must keep headroom (peak=$peak)")
            // The seam: wrapping from the last sample to the first must be an ordinary step —
            // quantized oscillators + the noise head-crossfade guarantee it.
            val seam = abs(s[s.size - 1] - s[0])
            assertTrue(seam < 3000, "$track loop seam pops (delta=$seam)")
        }
    }
}
