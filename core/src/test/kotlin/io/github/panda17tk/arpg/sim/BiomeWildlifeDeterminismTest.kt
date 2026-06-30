package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BiomeWildlifeDeterminismTest {
    @Test fun `populate is deterministic per biome and seed`() {
        for (biome in PlanetBiome.values()) {
            val a = SurfaceEcology.populate(biome, 1000f, 1000f, 2000f, 2000f, Rng(8L)).placements.map { it.key }
            val b = SurfaceEcology.populate(biome, 1000f, 1000f, 2000f, 2000f, Rng(8L)).placements.map { it.key }
            assertEquals(a, b, "$biome population should be deterministic")
        }
    }

    @Test fun `every placed wild animal actually belongs to the biome it is on`() {
        val cfg = GameConfig()
        for (biome in PlanetBiome.values()) {
            val placements = SurfaceEcology.populate(biome, 1000f, 1000f, 2000f, 2000f, Rng(8L)).placements
            for (p in placements) {
                val d = cfg.enemies[p.key] ?: continue
                if (d.lifeKind == LifeKind.WILDLIFE) {
                    assertTrue(d.biome == biome, "${p.key} (biome ${d.biome}) was placed on $biome")
                }
            }
        }
    }
}
