package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlanetContextDeterminismTest {
    @Test fun `contextFor is stable for a given id and biome`() {
        assertEquals(
            PlanetContext.contextFor(777L, PlanetBiome.NATURE),
            PlanetContext.contextFor(777L, PlanetBiome.NATURE),
        )
    }

    @Test fun `different ids can produce different contexts`() {
        val contexts = (0L until 40L).map { PlanetContext.contextFor(it, PlanetBiome.MAGMA) }.toSet()
        assertTrue(contexts.size > 1, "the id should vary the context")
    }

    @Test fun `NEUTRAL is a silent, story-less default`() {
        assertEquals(PlanetStorySeed.NONE, PlanetContext.NEUTRAL.storySeed)
        assertEquals(PlanetTemperament.SILENT, PlanetContext.NEUTRAL.temperament)
    }
}
