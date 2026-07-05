package io.github.panda17tk.arpg.map

import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.Tuning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.79 水域: deterministic bodies, biome-true, the pad stays dry, wading rules hold. */
class SurfaceWaterTest {
    private val w = 72f * Tuning.TILE
    private val h = 46f * Tuning.TILE

    @Test fun `the same landing always carves the same water`() {
        assertEquals(
            SurfaceWater.generate(PlanetBiome.NATURE, 7L, w, h),
            SurfaceWater.generate(PlanetBiome.NATURE, 7L, w, h),
        )
    }

    @Test fun `dry worlds stay dry and wet worlds do get water`() {
        for (seed in 0L..40L) {
            assertTrue(SurfaceWater.generate(PlanetBiome.MAGMA, seed, w, h).isEmpty, "magma must stay dry")
            assertTrue(SurfaceWater.generate(PlanetBiome.GAS, seed, w, h).isEmpty, "gas must stay dry")
            assertTrue(SurfaceWater.generate(PlanetBiome.LONELY, seed, w, h).isEmpty, "lonely must stay dry")
        }
        val natureWet = (0L..40L).count { !SurfaceWater.generate(PlanetBiome.NATURE, it, w, h).isEmpty }
        assertTrue(natureWet > 20, "nature should usually carry water (got $natureWet/41)")
        val iceWet = (0L..40L).count { SurfaceWater.generate(PlanetBiome.ICE, it, w, h).lakes.isNotEmpty() }
        assertTrue(iceWet > 25, "ice should usually hold frozen ponds (got $iceWet/41)")
        for (seed in 0L..40L) {
            assertTrue(SurfaceWater.generate(PlanetBiome.ICE, seed, w, h).frozen, "ice ponds are frozen")
        }
    }

    @Test fun `the landing pad and its approach never flood`() {
        for (b in listOf(PlanetBiome.NATURE, PlanetBiome.ICE, PlanetBiome.DEAD)) for (seed in 0L..60L) {
            val water = SurfaceWater.generate(b, seed, w, h)
            assertFalse(SurfaceWater.inWater(water, w / 2f, h / 2f), "$b seed $seed flooded the pad")
        }
    }

    @Test fun `wading is wet water only — lake hearts wade, ice and dry land never`() {
        // find a nature seed with a lake and check its centre
        var checked = false
        for (seed in 0L..60L) {
            val water = SurfaceWater.generate(PlanetBiome.NATURE, seed, w, h)
            val lake = water.lakes.firstOrNull() ?: continue
            assertTrue(SurfaceWater.inWater(water, lake.cx, lake.cy), "the lake's heart must be water")
            assertTrue(SurfaceWater.wadingAt(water, lake.cx, lake.cy), "open water must wade")
            assertFalse(SurfaceWater.inWater(water, lake.cx + lake.rx * 3f + Tuning.TILE, lake.cy), "far shore is dry")
            checked = true
            break
        }
        assertTrue(checked, "no nature lake found in 61 seeds")
        for (seed in 0L..60L) { // a frozen pond never slows anyone
            val ice = SurfaceWater.generate(PlanetBiome.ICE, seed, w, h)
            val pond = ice.lakes.firstOrNull() ?: continue
            assertTrue(SurfaceWater.inWater(ice, pond.cx, pond.cy))
            assertFalse(SurfaceWater.wadingAt(ice, pond.cx, pond.cy))
        }
        assertTrue(SurfaceWater.WADE_SLOW in 0.4f..0.9f)
    }

    @Test fun `rivers run bank to bank and their midstream is water`() {
        var checked = false
        for (seed in 0L..80L) {
            val water = SurfaceWater.generate(PlanetBiome.NATURE, seed, w, h)
            val rv = water.rivers.firstOrNull() ?: continue
            assertTrue(rv.points.first().first <= 0f + Tuning.TILE * 2f, "river starts at the west bank")
            assertTrue(rv.points.last().first >= w - Tuning.TILE * 2f, "river reaches the east bank")
            val (mx, my) = rv.points[rv.points.size / 2]
            assertTrue(SurfaceWater.inWater(water, mx, my), "midstream must be water")
            checked = true
            break
        }
        assertTrue(checked, "no nature river found in 81 seeds")
    }
}
