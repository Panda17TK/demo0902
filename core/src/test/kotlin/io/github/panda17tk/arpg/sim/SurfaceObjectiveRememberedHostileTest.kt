package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SurfaceObjectiveRememberedHostileTest {
    @Test fun `a remembered hostile planet shows the cold greeting`() {
        val line = SurfaceObjective.hudLine(PlanetBiome.NATURE, 3, PlanetSocietyState(hostility = 0.7f), PlanetContext.NEUTRAL, remembered = true)
        assertTrue(line.contains("敵として覚えている"), line)
    }

    @Test fun `without the remembered flag the greeting is suppressed`() {
        val line = SurfaceObjective.hudLine(PlanetBiome.NATURE, 3, PlanetSocietyState(hostility = 0.7f), PlanetContext.NEUTRAL, remembered = false)
        assertFalse(line.contains("覚えている"), line)
    }
}
