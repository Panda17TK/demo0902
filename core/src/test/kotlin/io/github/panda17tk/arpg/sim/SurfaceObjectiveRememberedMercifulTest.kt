package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SurfaceObjectiveRememberedMercifulTest {
    @Test fun `a remembered indebted planet shows the warm greeting`() {
        val line = SurfaceObjective.hudLine(PlanetBiome.NATURE, 0, PlanetSocietyState(mercy = 0.6f), PlanetContext.NEUTRAL, remembered = true)
        assertTrue(line.contains("借りを覚えている"), line)
    }

    @Test fun `a hostile memory outranks a merciful one in the greeting`() {
        val line = SurfaceObjective.hudLine(PlanetBiome.NATURE, 0, PlanetSocietyState(hostility = 0.7f, mercy = 0.6f), PlanetContext.NEUTRAL, remembered = true)
        assertTrue(line.contains("敵として覚えている"), line)
    }
}
