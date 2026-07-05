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
    private fun society(b: PlanetBiome, seed: Long = 1L) =
        SurfaceEcology.populate(b, 1000f, 1000f, 4000f, 4000f, Rng(seed))
    private fun place(b: PlanetBiome, seed: Long = 1L) = society(b, seed).placements
    private fun facilities(b: PlanetBiome, seed: Long = 1L) = society(b, seed).facilities

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

    @Test fun `a lonely asteroid stays sparse`() {
        // A lone leader (+maybe a monk) and a few rare wild drifters — still far emptier than a full biome.
        for (seed in 1L..8L) {
            val lonely = place(PlanetBiome.LONELY, seed).size
            assertTrue(lonely <= 11, "lonely should stay sparse, was $lonely") // v2.82: a few more drifters, still far under a full biome
            assertTrue(lonely < place(PlanetBiome.NATURE, seed).size, "lonely should be sparser than nature")
        }
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
        assertEquals(society(PlanetBiome.MAGMA, 5L), society(PlanetBiome.MAGMA, 5L))
    }

    // --- facilities (Sprint L) ---

    @Test fun `every biome builds at least one facility`() {
        for (b in PlanetBiome.values()) assertTrue(facilities(b).isNotEmpty(), "no facility for $b")
    }

    @Test fun `magma builds a crater and ice a dais`() {
        assertTrue(facilities(PlanetBiome.MAGMA).any { it.kind == FacilityKind.CRATER })
        assertTrue(facilities(PlanetBiome.ICE).any { it.kind == FacilityKind.DAIS })
    }

    @Test fun `the dead world is dotted with ruins`() {
        assertTrue(facilities(PlanetBiome.DEAD).count { it.kind == FacilityKind.RUIN } >= 1)
    }

    @Test fun `the leader sits on the central facility`() {
        val s = society(PlanetBiome.MAGMA, 2L)
        val king = s.placements.first() // volcano_king is added first (dist 0 from the camp heart)
        val crater = s.facilities.first { it.kind == FacilityKind.CRATER }
        assertEquals(crater.x, king.x, 1e-3f)
        assertEquals(crater.y, king.y, 1e-3f)
    }

    @Test fun `facilities stay inside the arena`() {
        for (b in PlanetBiome.values()) for (f in facilities(b)) {
            assertTrue(f.x in 0f..4000f && f.y in 0f..4000f, "${f.kind} at ${f.x},${f.y}")
        }
    }
}
