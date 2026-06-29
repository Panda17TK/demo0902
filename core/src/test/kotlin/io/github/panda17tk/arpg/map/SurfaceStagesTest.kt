package io.github.panda17tk.arpg.map

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SurfaceStagesTest {
    @Test fun `surface arena is fully bordered with walls`() {
        val s = SurfaceStages.forBiome(Biome.GRASS, 1L)
        assertTrue(s.rows.first().all { it == '#' }, "top border")
        assertTrue(s.rows.last().all { it == '#' }, "bottom border")
        assertTrue(s.rows.all { it.first() == '#' && it.last() == '#' }, "side borders")
    }

    @Test fun `surface arena has a player spawn`() {
        assertTrue(SurfaceStages.forBiome(Biome.MAGMA, 2L).rows.any { 'P' in it })
    }

    @Test fun `same seed yields an identical arena`() {
        assertEquals(SurfaceStages.forBiome(Biome.SNOW, 5L).rows, SurfaceStages.forBiome(Biome.SNOW, 5L).rows)
    }
}
