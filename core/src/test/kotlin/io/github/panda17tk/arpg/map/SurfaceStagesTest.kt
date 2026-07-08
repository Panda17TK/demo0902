package io.github.panda17tk.arpg.map

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SurfaceStagesTest {
    @Test fun `surface arena keeps its outer ring open — the world wraps there`() {
        val s = SurfaceStages.forBiome(PlanetBiome.NATURE, 1L)
        // v2.83: the rim is guaranteed floor (wrap lanes) — obstacles never reach it
        assertTrue(s.rows.first().all { it != '#' }, "top rim must stay open")
        assertTrue(s.rows.last().all { it != '#' }, "bottom rim must stay open")
        assertTrue(s.rows.all { it.first() != '#' && it.last() != '#' }, "side rims must stay open")
    }

    @Test fun `surface arena has a player spawn`() {
        assertTrue(SurfaceStages.forBiome(PlanetBiome.MAGMA, 2L).rows.any { 'P' in it })
    }

    @Test fun `same seed yields an identical arena`() {
        assertEquals(SurfaceStages.forBiome(PlanetBiome.ICE, 5L).rows, SurfaceStages.forBiome(PlanetBiome.ICE, 5L).rows)
    }

    @Test fun `every planet biome yields a valid wrappable arena with a spawn`() {
        for (b in PlanetBiome.values()) {
            val s = SurfaceStages.forBiome(b, 9L)
            assertTrue(s.rows.first().all { it != '#' } && s.rows.last().all { it != '#' }, "top/bottom rims open for $b")
            assertTrue(s.rows.all { it.first() != '#' && it.last() != '#' }, "side rims open for $b")
            assertTrue(s.rows.any { 'P' in it }, "spawn for $b")
            assertEquals("surface_${b.name.lowercase()}", s.id, "id for $b")
        }
    }

    @Test fun `surfaces grew — a standard planet spans at least 100x64 tiles`() {
        // v2.126 惑星の拡張
        val s = SurfaceStages.forBiome(PlanetBiome.NATURE, 7L)
        assertTrue(s.rows.size >= 64 && s.rows.first().length >= 100, "got ${s.rows.first().length}x${s.rows.size}")
    }

    @Test fun `a lonely planet makes a smaller arena than a gas planet`() {
        val lonely = SurfaceStages.forBiome(PlanetBiome.LONELY, 3L)
        val gas = SurfaceStages.forBiome(PlanetBiome.GAS, 3L)
        assertTrue(lonely.rows.size < gas.rows.size, "lonely=${lonely.rows.size} should be smaller than gas=${gas.rows.size}")
    }
}
