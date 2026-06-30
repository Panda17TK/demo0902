package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SurfaceEcologyWildlifeTest {
    private val enemies = GameConfig().enemies
    private fun nature(seed: Long = 1L) =
        SurfaceEcology.populate(PlanetBiome.NATURE, 1000f, 1000f, 4000f, 4000f, Rng(seed)).placements
    private fun wildRoles(seed: Long = 1L) =
        nature(seed).mapNotNull { enemies[it.key] }.filter { it.lifeKind == LifeKind.WILDLIFE }.map { it.wildRole }.toSet()

    @Test fun `nature includes wild prey`() {
        assertTrue(WildRole.PREY in wildRoles(), "no wild prey on the nature surface")
    }

    @Test fun `nature includes a wild predator`() {
        assertTrue(WildRole.PREDATOR in wildRoles(), "no wild predator on the nature surface")
    }

    @Test fun `nature includes a wild herd`() {
        assertTrue(WildRole.HERD in wildRoles(), "no wild herd on the nature surface")
    }

    @Test fun `nature places both a sapient society and wildlife`() {
        val defs = nature().mapNotNull { enemies[it.key] }
        assertTrue(defs.any { it.lifeKind == LifeKind.SAPIENT }, "no sapient society member placed")
        assertTrue(defs.any { it.lifeKind == LifeKind.WILDLIFE }, "no wildlife placed")
    }

    @Test fun `the same seed yields the same surface`() {
        assertEquals(nature(7L), nature(7L))
    }
}
