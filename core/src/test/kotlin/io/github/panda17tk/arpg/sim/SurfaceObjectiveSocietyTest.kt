package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SurfaceObjectiveSocietyTest {
    private fun line(soc: PlanetSocietyState, elites: Int = 1) = SurfaceObjective.hudLine(PlanetBiome.NATURE, elites, soc)

    @Test fun `a harmed-child message outranks the routine objective`() {
        val soc = PlanetSocietyState().apply { onChildHarmed() }
        assertTrue(line(soc, elites = 2).contains("守護者"), line(soc, 2))
    }

    @Test fun `a killed child outranks a harmed one`() {
        val soc = PlanetSocietyState().apply { onChildHarmed(); onChildKilled() }
        assertTrue(line(soc).contains("部族は怒っている"), line(soc))
    }

    @Test fun `repelling a predator near a child shows in the objective`() {
        val soc = PlanetSocietyState().apply { onPredatorRepelledNearChild() }
        assertTrue(line(soc).contains("森はあなたを見ている"), line(soc))
    }

    @Test fun `killing the apex shows in the objective`() {
        val soc = PlanetSocietyState().apply { onApexKilled() }
        assertTrue(line(soc).contains("生態系が揺らいでいる"), line(soc))
    }

    @Test fun `with nothing remembered the routine objective shows`() {
        assertTrue(line(PlanetSocietyState(), elites = 3).contains("主を倒せ"), line(PlanetSocietyState(), 3))
    }
}
