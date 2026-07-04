package io.github.panda17tk.arpg.map

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BiomeTest {
    @Test fun `biome is constant within a region and deterministic`() {
        assertEquals(Biomes.of(0, 0), Biomes.of(0, 0))
        assertEquals(Biomes.of(0, 0), Biomes.of(3, 5)) // same region (0..8 in both axes)
    }

    @Test fun `the map shows several different biomes`() {
        val seen = HashSet<Biome>()
        for (rx in 0 until 50) for (ry in 0 until 50) seen.add(Biomes.of(rx * 9, ry * 9))
        assertTrue(seen.size >= 3, "only saw $seen")
    }

    // --- surface material leans to the planet biome (Sprint M) ---

    private fun frac(b: PlanetBiome, m: Biome): Double {
        var hit = 0; var n = 0
        for (tx in 0 until 60) for (ty in 0 until 60) { n++; if (Biomes.surface(b, tx, ty) == m) hit++ }
        return hit.toDouble() / n
    }

    @Test fun `surface terrain leans to the planet's signature material`() {
        assertTrue(frac(PlanetBiome.NATURE, Biome.GRASS) > 0.5, "nature should be grass-heavy")
        assertTrue(frac(PlanetBiome.MAGMA, Biome.MAGMA) > 0.5, "magma should be lava-heavy")
        assertTrue(frac(PlanetBiome.ICE, Biome.SNOW) > 0.5, "ice should be snow-heavy")
        // v2.39: the dead world is rock and ash (with rare crystal) — and never a live hazard.
        assertTrue(frac(PlanetBiome.DEAD, Biome.ROCK) + frac(PlanetBiome.DEAD, Biome.ASH) > 0.7, "the dead world is rock+ash")
        assertEquals(0.0, frac(PlanetBiome.DEAD, Biome.MAGMA) + frac(PlanetBiome.DEAD, Biome.SNOW) + frac(PlanetBiome.DEAD, Biome.GRASS), 1e-9)
    }

    @Test fun `surface material is deterministic`() {
        assertEquals(Biomes.surface(PlanetBiome.ICE, 5, 7), Biomes.surface(PlanetBiome.ICE, 5, 7))
    }
}
