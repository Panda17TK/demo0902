package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SurfaceObjectiveMemoryTest {
    private fun line(s: PlanetSocietyState, elites: Int = 0) = SurfaceObjective.hudLine(PlanetBiome.NATURE, elites, s)

    @Test fun `a killed child dominates the line, even with elites still alive`() {
        assertTrue(line(PlanetSocietyState().also { it.childKilled = true }, elites = 5).contains("怒"))
    }

    @Test fun `a slain apex shows the ecosystem is shaken`() {
        assertTrue(line(PlanetSocietyState().also { it.apexKilled = true }).contains("揺ら"))
    }

    @Test fun `repelling a predator near a child is remembered kindly`() {
        assertTrue(line(PlanetSocietyState().also { it.predatorKilledNearChild = true }).contains("見て"))
    }

    @Test fun `a blank society shows the plain master objective`() {
        assertTrue(line(PlanetSocietyState(), elites = 2).contains("主を倒せ"))
    }
}
