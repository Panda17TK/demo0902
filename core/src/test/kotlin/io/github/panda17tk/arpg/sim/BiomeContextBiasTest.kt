package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BiomeContextBiasTest {
    private val ids = (1L..200L).toList()

    @Test fun `nature planets are never proud or vengeful`() {
        val temps = ids.map { PlanetContext.contextFor(it, PlanetBiome.NATURE).temperament }.toSet()
        assertTrue(temps.all { it in PlanetContext.temperaments(PlanetBiome.NATURE) })
        assertTrue(PlanetTemperament.PROUD !in temps && PlanetTemperament.VENGEFUL !in temps)
    }

    @Test fun `magma temperaments and sacreds stay on the magma roster`() {
        val temps = ids.map { PlanetContext.contextFor(it, PlanetBiome.MAGMA).temperament }.toSet()
        val sacreds = ids.map { PlanetContext.contextFor(it, PlanetBiome.MAGMA).sacredThing }.toSet()
        assertTrue(temps.all { it in PlanetContext.temperaments(PlanetBiome.MAGMA) })
        assertTrue(sacreds.all { it in PlanetContext.sacredThings(PlanetBiome.MAGMA) })
    }

    @Test fun `every biome only ever yields its allowed story seeds`() {
        for (biome in PlanetBiome.values()) {
            val seeds = ids.map { PlanetContext.contextFor(it, biome).storySeed }.toSet()
            assertTrue(seeds.all { it in PlanetContext.storySeeds(biome) }, "$biome produced an off-roster story seed")
        }
    }

    @Test fun `the bias actually produces variety across many ids`() {
        val temps = ids.map { PlanetContext.contextFor(it, PlanetBiome.ICE).temperament }.toSet()
        assertTrue(temps.size >= 2, "expected variety across ICE temperaments")
    }
}
