package io.github.panda17tk.arpg.map

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
}
