package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SilentMonasteryPlacementTest {
    private fun ctx(s: PlanetStorySeed) = PlanetContext(PlanetTemperament.SILENT, SacredThing.SILENCE, s)
    private fun pop(biome: PlanetBiome, s: PlanetStorySeed) =
        SurfaceEcology.populate(biome, 1000f, 1000f, 2000f, 2000f, Rng(5L), ctx(s))

    @Test fun `SILENT_MONASTERY seats a monk and stays a low-combat, mostly-passive place`() {
        val mon = pop(PlanetBiome.LONELY, PlanetStorySeed.SILENT_MONASTERY)
        assertTrue(mon.placements.any { it.key == "star_monk" }, "a monk presides")
        // the silence: only the lone leader is a combatant; everyone added is passive
        assertTrue(mon.placements.count { !it.passive } <= 2, "low combat density")
    }

    @Test fun `the monastery adds passive watchers, not fighters`() {
        val none = pop(PlanetBiome.DEAD, PlanetStorySeed.NONE)
        val mon = pop(PlanetBiome.DEAD, PlanetStorySeed.SILENT_MONASTERY)
        assertTrue(mon.placements.size > none.placements.size, "the monastery adds quiet presences")
        assertTrue(mon.placements.count { !it.passive } <= none.placements.count { !it.passive } + 0, "no new fighters")
    }
}
