package io.github.panda17tk.arpg.map

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SurfaceStagesTest {
    @Test fun `surface arena is fully bordered with walls`() {
        val s = SurfaceStages.forBiome(PlanetBiome.NATURE, 1L)
        assertTrue(s.rows.first().all { it == '#' }, "top border")
        assertTrue(s.rows.last().all { it == '#' }, "bottom border")
        assertTrue(s.rows.all { it.first() == '#' && it.last() == '#' }, "side borders")
    }

    @Test fun `surface arena has a player spawn`() {
        assertTrue(SurfaceStages.forBiome(PlanetBiome.MAGMA, 2L).rows.any { 'P' in it })
    }

    @Test fun `same seed yields an identical arena`() {
        assertEquals(SurfaceStages.forBiome(PlanetBiome.ICE, 5L).rows, SurfaceStages.forBiome(PlanetBiome.ICE, 5L).rows)
    }

    @Test fun `every planet biome yields a valid bordered arena with a spawn`() {
        for (b in PlanetBiome.values()) {
            val s = SurfaceStages.forBiome(b, 9L)
            assertTrue(s.rows.first().all { it == '#' } && s.rows.last().all { it == '#' }, "top/bottom borders for $b")
            assertTrue(s.rows.all { it.first() == '#' && it.last() == '#' }, "side borders for $b")
            assertTrue(s.rows.any { 'P' in it }, "spawn for $b")
            assertEquals("surface_${b.name.lowercase()}", s.id, "id for $b")
        }
    }

    @Test fun `a lonely planet makes a smaller arena than a gas planet`() {
        val lonely = SurfaceStages.forBiome(PlanetBiome.LONELY, 3L)
        val gas = SurfaceStages.forBiome(PlanetBiome.GAS, 3L)
        assertTrue(lonely.rows.size < gas.rows.size, "lonely=${lonely.rows.size} should be smaller than gas=${gas.rows.size}")
    }
}
