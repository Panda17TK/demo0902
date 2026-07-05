package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.74 天候: deterministic per planet, biome-appropriate, and pure in time. */
class WeatherTest {
    @Test fun `a planet's climate never changes between landings`() {
        for (id in 0L..40L) for (b in PlanetBiome.entries) {
            assertEquals(Weather.kindFor(id, b), Weather.kindFor(id, b))
        }
    }

    @Test fun `each biome rains only what belongs to it — and some skies stay clear`() {
        val allowed = mapOf(
            PlanetBiome.NATURE to setOf(WeatherKind.CLEAR, WeatherKind.RAIN),
            PlanetBiome.GAS to setOf(WeatherKind.CLEAR, WeatherKind.RAIN),
            PlanetBiome.ICE to setOf(WeatherKind.CLEAR, WeatherKind.SNOW),
            PlanetBiome.MAGMA to setOf(WeatherKind.CLEAR, WeatherKind.ASH),
            PlanetBiome.DEAD to setOf(WeatherKind.CLEAR, WeatherKind.ASH),
            PlanetBiome.LONELY to setOf(WeatherKind.CLEAR, WeatherKind.DUSTWIND),
        )
        for (b in PlanetBiome.entries) {
            val seen = (0L..200L).map { Weather.kindFor(it, b) }.toSet()
            assertEquals(allowed.getValue(b), seen, "biome $b grew the wrong sky: $seen")
        }
    }

    @Test fun `particles stay in the unit square and drift deterministically`() {
        for (kind in WeatherKind.entries) {
            if (kind == WeatherKind.CLEAR) continue
            val p = Weather.paramsFor(kind)
            assertTrue(p.count > 0 && p.size > 0f)
            for (i in 0 until p.count step 7) for (t in listOf(0f, 3.3f, 47.9f)) {
                val (x, y) = Weather.pos(i, t, p)
                assertTrue(x in 0f..1f && y in 0f..1f, "$kind particle $i strayed to ($x,$y)")
                assertEquals(Weather.pos(i, t, p), Weather.pos(i, t, p))
            }
        }
    }

    @Test fun `the sky reshapes the food web the way the fiction says`() {
        // v2.75: rain shelters the hunters; snow and ash send the herds in; clear changes nothing.
        val clear = Weather.ecologyTweaks(WeatherKind.CLEAR)
        assertEquals(1f, clear.predatorMul); assertEquals(1f, clear.grazerMul)
        assertTrue(Weather.ecologyTweaks(WeatherKind.RAIN).predatorMul < 1f)
        assertEquals(1f, Weather.ecologyTweaks(WeatherKind.RAIN).grazerMul)
        assertTrue(Weather.ecologyTweaks(WeatherKind.SNOW).grazerMul < 1f)
        assertTrue(Weather.ecologyTweaks(WeatherKind.ASH).grazerMul < 1f)
        for (k in WeatherKind.entries) {
            val e = Weather.ecologyTweaks(k)
            assertTrue(e.predatorMul in 0.3f..1f && e.grazerMul in 0.3f..1f, "$k tweaks out of band")
        }
    }

    @Test fun `rain falls hard, snow floats, the dust wind blows sideways`() {
        val rain = Weather.paramsFor(WeatherKind.RAIN)
        val snow = Weather.paramsFor(WeatherKind.SNOW)
        val dust = Weather.paramsFor(WeatherKind.DUSTWIND)
        assertTrue(rain.fallPerSec > snow.fallPerSec * 3f, "rain must fall much faster than snow")
        assertTrue(kotlin.math.abs(dust.driftPerSec) > dust.fallPerSec * 5f, "dust wind is horizontal")
        assertTrue(snow.sway > 0f && rain.sway == 0f)
        assertEquals(0, Weather.paramsFor(WeatherKind.CLEAR).count)
    }
}
