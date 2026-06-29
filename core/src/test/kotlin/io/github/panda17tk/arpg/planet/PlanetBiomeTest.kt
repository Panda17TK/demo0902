package io.github.panda17tk.arpg.planet

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlanetBiomeTest {
    @Test fun `every planet biome has a non-blank HUD label`() {
        for (b in PlanetBiome.values()) assertTrue(b.displayName.isNotBlank(), "no label for $b")
    }

    @Test fun `labels read as planet types`() {
        assertEquals("自然惑星", PlanetBiome.NATURE.displayName)
        assertEquals("火山惑星", PlanetBiome.MAGMA.displayName)
        assertEquals("氷惑星", PlanetBiome.ICE.displayName)
        assertEquals("ガス惑星", PlanetBiome.GAS.displayName)
        assertEquals("死の惑星", PlanetBiome.DEAD.displayName)
        assertEquals("孤独な小惑星", PlanetBiome.LONELY.displayName)
    }

    @Test fun `labels are unique per biome`() {
        val labels = PlanetBiome.values().map { it.displayName }
        assertEquals(labels.size, labels.toSet().size, "duplicate labels in $labels")
    }
}
