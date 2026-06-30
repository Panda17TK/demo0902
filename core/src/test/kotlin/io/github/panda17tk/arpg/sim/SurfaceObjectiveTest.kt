package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SurfaceObjectiveTest {
    @Test fun `while masters live the objective names the planet and the count`() {
        val s = SurfaceObjective.hudLine(PlanetBiome.NATURE, 2)
        assertTrue(s.contains("自然惑星"), s)
        assertTrue(s.contains("2"), s)
    }

    @Test fun `a subdued planet points back to the escape pad`() {
        val s = SurfaceObjective.hudLine(PlanetBiome.MAGMA, 0)
        assertTrue(s.contains("火山惑星"), s)
        assertTrue(s.contains("脱出パッド"), s)
    }

    @Test fun `every biome yields a non-blank line in both states`() {
        for (b in PlanetBiome.values()) {
            assertTrue(SurfaceObjective.hudLine(b, 3).isNotBlank(), "$b active")
            assertTrue(SurfaceObjective.hudLine(b, 0).isNotBlank(), "$b cleared")
        }
    }
}
