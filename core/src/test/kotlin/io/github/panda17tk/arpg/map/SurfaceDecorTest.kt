package io.github.panda17tk.arpg.map

import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.Tuning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.78 地表の装飾: deterministic scatter, in-bounds, biome-true palettes, a clear pad. */
class SurfaceDecorTest {
    private val w = 72f * Tuning.TILE
    private val h = 46f * Tuning.TILE

    @Test fun `the same landing always grows the same garden`() {
        assertEquals(
            SurfaceDecor.scatter(PlanetBiome.NATURE, 7L, w, h),
            SurfaceDecor.scatter(PlanetBiome.NATURE, 7L, w, h),
        )
    }

    @Test fun `decor stays inside the arena and off the landing pad`() {
        val margin = Tuning.TILE
        val clearR = Tuning.TILE * 3f
        for (b in PlanetBiome.entries) for (seed in listOf(1L, 42L, 999L)) {
            val ds = SurfaceDecor.scatter(b, seed, w, h)
            assertTrue(ds.size > 20, "$b too sparse (${ds.size})")
            for (d in ds) {
                assertTrue(d.x in margin..(w - margin) && d.y in margin..(h - margin), "$b decor out of bounds: $d")
                val dx = d.x - w / 2f; val dy = d.y - h / 2f
                assertTrue(dx * dx + dy * dy >= clearR * clearR, "$b decor crowds the pad: $d")
                assertTrue(d.scale in 0.7f..1.4f && d.hue in 0f..1f)
            }
        }
    }

    @Test fun `each biome grows its own furniture`() {
        fun kinds(b: PlanetBiome) = (0L..20L).flatMap { SurfaceDecor.scatter(b, it, w, h) }.map { it.kind }.toSet()
        val nature = kinds(PlanetBiome.NATURE)
        assertTrue(DecorKind.TREE in nature && DecorKind.FLOWER in nature)
        assertTrue(DecorKind.VENT !in nature && DecorKind.ICE_SPIKE !in nature)
        val magma = kinds(PlanetBiome.MAGMA)
        assertTrue(DecorKind.VENT in magma && DecorKind.TREE !in magma)
        val ice = kinds(PlanetBiome.ICE)
        assertTrue(DecorKind.ICE_SPIKE in ice && DecorKind.TREE !in ice)
        val gas = kinds(PlanetBiome.GAS)
        assertTrue(DecorKind.SPORE in gas)
        val dead = kinds(PlanetBiome.DEAD)
        assertTrue(DecorKind.DEAD_TREE in dead && DecorKind.CAIRN in dead && DecorKind.TREE !in dead)
        val lonely = kinds(PlanetBiome.LONELY)
        assertTrue(DecorKind.CAIRN in lonely && DecorKind.TREE !in lonely)
    }

    @Test fun `different seeds lay different gardens`() {
        val a = SurfaceDecor.scatter(PlanetBiome.NATURE, 1L, w, h)
        val b = SurfaceDecor.scatter(PlanetBiome.NATURE, 2L, w, h)
        assertTrue(a != b, "two landings should not share one garden")
    }
}
