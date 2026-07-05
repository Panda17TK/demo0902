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
