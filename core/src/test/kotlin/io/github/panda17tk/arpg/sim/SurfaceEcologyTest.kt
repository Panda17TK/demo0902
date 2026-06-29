package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.config.FamilyRole
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SurfaceEcologyTest {
    private val enemies = GameConfig().enemies
    private fun place(b: PlanetBiome, seed: Long = 1L) =
        SurfaceEcology.populate(b, 1000f, 1000f, 4000f, 4000f, Rng(seed))

    @Test fun `nature lands you among a child a guardian and a king`() {
        val roles = place(PlanetBiome.NATURE).mapNotNull { enemies[it.key]?.familyRole }.toSet()
        assertTrue(FamilyRole.CHILD in roles, "no child")
        assertTrue(FamilyRole.GUARDIAN in roles, "no guardian")
        assertTrue(FamilyRole.KING in roles, "no king")
    }

    @Test fun `each biome only places its own inhabitants`() {
        for (b in PlanetBiome.values()) {
            for (p in place(b)) assertEquals(b, enemies[p.key]?.biome, "${p.key} is not a $b creature")
        }
    }

    @Test fun `gas dwellers all ignore gravity`() {
        val gas = place(PlanetBiome.GAS)
        assertTrue(gas.isNotEmpty())
        for (p in gas) assertEquals(0f, enemies[p.key]?.gravityResponse ?: -1f, 1e-6f, p.key)
    }

    @Test fun `a lonely asteroid is a sparse event`() {
        for (seed in 1L..8L) assertTrue(place(PlanetBiome.LONELY, seed).size <= 3)
    }

    @Test fun `every biome is populated`() {
        for (b in PlanetBiome.values()) assertTrue(place(b).isNotEmpty(), "no inhabitants for $b")
    }

    @Test fun `placements stay inside the arena`() {
        for (b in PlanetBiome.values()) for (p in place(b)) {
            assertTrue(p.x in 0f..4000f && p.y in 0f..4000f, "${p.key} at ${p.x},${p.y}")
        }
    }

    @Test fun `same seed yields the same society`() {
        assertEquals(place(PlanetBiome.MAGMA, 5L), place(PlanetBiome.MAGMA, 5L))
    }
}
