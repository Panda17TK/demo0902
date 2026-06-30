package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SurfaceObjectiveDefaultStillWorksTest {
    @Test fun `the default call (no context, not remembered) still shows the elite objective`() {
        assertTrue(SurfaceObjective.hudLine(PlanetBiome.NATURE, 2).contains("主を倒せ"))
    }

    @Test fun `a child kill still dominates under the default context`() {
        assertTrue(SurfaceObjective.hudLine(PlanetBiome.NATURE, 5, PlanetSocietyState().also { it.childKilled = true }).contains("怒"))
    }

    @Test fun `a cleared world still points to the escape pad`() {
        val s = PlanetSocietyState().also { it.leaderDefeated = true; it.relicClaimed = true }
        assertTrue(SurfaceObjective.hudLine(PlanetBiome.NATURE, 0, s).contains("脱出パッド"))
    }
}
