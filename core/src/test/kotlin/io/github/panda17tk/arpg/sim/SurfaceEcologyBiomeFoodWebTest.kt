package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SurfaceEcologyBiomeFoodWebTest {
    private fun keys(biome: PlanetBiome) =
        SurfaceEcology.populate(biome, 1000f, 1000f, 2000f, 2000f, Rng(4L)).placements.map { it.key }.toSet()

    @Test fun `magma has prey, a herd, a predator and hatchlings`() {
        val k = keys(PlanetBiome.MAGMA)
        assertTrue("ash_lizard" in k && "basalt_ram" in k && "lava_serpent" in k && "crater_hatchling" in k, "magma food web: $k")
    }

    @Test fun `ice has a herd, a predator and a rare apex`() {
        val k = keys(PlanetBiome.ICE)
        assertTrue("ice_muskox" in k && "white_stalker" in k && "frost_worm" in k, "ice food web: $k")
    }

    @Test fun `gas has a floating food web`() {
        val k = keys(PlanetBiome.GAS)
        assertTrue("cloud_plankton" in k && "thunder_eel" in k, "gas food web: $k")
    }

    @Test fun `dead has a scavenger-mimic ecosystem`() {
        val k = keys(PlanetBiome.DEAD)
        assertTrue(("bone_rat" in k || "ash_crow" in k) && ("grave_mimic" in k || "ruin_parasite" in k), "dead ecosystem: $k")
    }

    @Test fun `lonely stays sparser than the crowded dead world`() {
        val dead = SurfaceEcology.populate(PlanetBiome.DEAD, 1000f, 1000f, 2000f, 2000f, Rng(4L)).placements.size
        val lonely = SurfaceEcology.populate(PlanetBiome.LONELY, 1000f, 1000f, 2000f, 2000f, Rng(4L)).placements.size
        assertTrue(lonely < dead, "lonely ($lonely) should be sparser than dead ($dead)")
    }
}
