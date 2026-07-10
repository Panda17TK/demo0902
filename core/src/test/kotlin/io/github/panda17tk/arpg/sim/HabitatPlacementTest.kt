package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.map.Lake
import io.github.panda17tk.arpg.map.River
import io.github.panda17tk.arpg.map.SurfaceWater
import io.github.panda17tk.arpg.map.WaterBodies
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.hypot

/**
 * v2.133 適所の生態 — creatures spawn where their life actually is: nest kin at the nest,
 * the grave's mimic at the grave, ruin crawlers among the ruins, and shore points sit on
 * the bank rather than in the wade. Pure layers only.
 */
class HabitatPlacementTest {
    @Test fun `nest kin gather at the nest and grave kin at the grave`() {
        for (seed in 1L..3L) {
            val nature = SurfaceEcology.populate(PlanetBiome.NATURE, 1000f, 1000f, 3400f, 2200f, Rng(seed))
            val nest = nature.facilities.first { it.kind == FacilityKind.NEST }
            val kin = nature.placements.filter { it.key == "nest_mother" || it.key == "forest_hatchling" }
            assertTrue(kin.isNotEmpty(), "the nest has its kin")
            for (p in kin) {
                assertTrue(hypot(p.x - nest.x, p.y - nest.y) <= nest.radius * 1.2f,
                    "seed $seed: ${p.key} keeps to the nest (${hypot(p.x - nest.x, p.y - nest.y)})")
            }

            val dead = SurfaceEcology.populate(PlanetBiome.DEAD, 1000f, 1000f, 3400f, 2200f, Rng(seed))
            val grave = dead.facilities.first { it.kind == FacilityKind.GRAVE }
            for (p in dead.placements.filter { it.key == "grave_mimic" }) {
                assertTrue(hypot(p.x - grave.x, p.y - grave.y) <= grave.radius * 1.2f,
                    "seed $seed: the mimic lurks at the grave")
            }
            val ruins = dead.facilities.filter { it.kind == FacilityKind.RUIN }
            assertTrue(ruins.isNotEmpty())
            for (p in dead.placements.filter { it.key == "ruin_parasite" || it.key == "crypt_beetle" }) {
                assertTrue(ruins.any { hypot(p.x - it.x, p.y - it.y) <= it.radius * 1.2f },
                    "seed $seed: ${p.key} crawls among the ruins")
            }
        }
    }

    @Test fun `the magma nest and the frozen pond hold their kin too`() {
        // MAGMA: the crater hatchlings gather at the nest facility (v2.133 anchor, ICE/MAGMA gap from the audit)
        for (seed in 1L..3L) {
            val magma = SurfaceEcology.populate(PlanetBiome.MAGMA, 1000f, 1000f, 3400f, 2200f, Rng(seed))
            val nest = magma.facilities.first { it.kind == FacilityKind.NEST }
            for (p in magma.placements.filter { it.key == "crater_hatchling" }) {
                assertTrue(hypot(p.x - nest.x, p.y - nest.y) <= nest.radius * 1.2f, "seed $seed: the hatchling keeps to the nest")
            }
        }
        // ICE: the frozen ponds are real water bodies and their shore points sit dry beside them
        val ice = io.github.panda17tk.arpg.map.SurfaceWater.generate(PlanetBiome.ICE, 4L, 3400f, 2200f)
        assertTrue(ice.frozen && ice.lakes.isNotEmpty(), "the ice world keeps its ponds")
        val lake = ice.lakes.first()
        val shore = io.github.panda17tk.arpg.map.SurfaceWater.nearestShore(ice, lake.cx, lake.cy)!!
        assertTrue(!io.github.panda17tk.arpg.map.SurfaceWater.inWater(ice, shore.first, shore.second), "the calf's bank is beside the pond, not on it")
    }

    @Test fun `the nearest shore stands on the bank, not in the wade`() {
        val lakeWorld = WaterBodies(lakes = listOf(Lake(500f, 500f, 100f, 60f)), rivers = emptyList(), frozen = false)
        val (sx, sy) = SurfaceWater.nearestShore(lakeWorld, 500f, 500f)!!
        assertFalse(SurfaceWater.inWater(lakeWorld, sx, sy), "the bank is dry")
        assertTrue(hypot(sx - 500f, sy - 500f) < 160f, "the bank hugs the lake")

        val riverWorld = WaterBodies(
            lakes = emptyList(),
            rivers = listOf(River(listOf(0f to 300f, 50f to 300f, 100f to 300f), width = 40f)),
            frozen = false,
        )
        val (rx, ry) = SurfaceWater.nearestShore(riverWorld, 60f, 310f)!!
        assertFalse(SurfaceWater.inWater(riverWorld, rx, ry), "the riverbank is dry")
        assertTrue(hypot(rx - 60f, ry - 310f) < 80f, "the bank hugs the river")

        assertTrue(SurfaceWater.nearestShore(WaterBodies.NONE, 10f, 10f) == null, "a dry world has no shore")
    }
}
