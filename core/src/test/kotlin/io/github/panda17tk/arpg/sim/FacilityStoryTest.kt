package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FacilityStoryTest {
    private fun facs(b: PlanetBiome) =
        SurfaceEcology.populate(b, 1000f, 1000f, 2000f, 2000f, Rng(3L)).facilities.map { it.kind }.toSet()

    @Test fun `biomes with hatchlings anchor a nest`() {
        assertTrue(FacilityKind.NEST in facs(PlanetBiome.NATURE))
        assertTrue(FacilityKind.NEST in facs(PlanetBiome.MAGMA))
        assertTrue(FacilityKind.NEST in facs(PlanetBiome.GAS))
    }

    @Test fun `the dead world has a grave, the living one does not`() {
        assertTrue(FacilityKind.GRAVE in facs(PlanetBiome.DEAD))
        assertFalse(FacilityKind.GRAVE in facs(PlanetBiome.NATURE))
    }

    @Test fun `every biome marks a relic altar`() {
        for (b in PlanetBiome.values()) assertTrue(FacilityKind.RELIC_ALTAR in facs(b), "$b has no relic altar")
    }
}
