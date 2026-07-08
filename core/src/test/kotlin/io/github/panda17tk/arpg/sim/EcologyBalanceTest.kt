package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * v2.132 生態系の力学 — the wild outnumbers the sapient camps, and the bravest hunters
 * no longer respect the camp line. Pure layers only: [SurfaceEcology] and [Predation].
 */
class EcologyBalanceTest {
    @Test fun `every biome fields more wildlife than sapient society`() {
        val defs = GameConfig().enemies
        for (biome in PlanetBiome.entries) {
            for (seed in 1L..4L) {
                val soc = SurfaceEcology.populate(biome, 1000f, 1000f, 3400f, 2200f, Rng(seed))
                var wild = 0; var other = 0
                for (p in soc.placements) {
                    val d = defs[p.key] ?: continue
                    if (d.lifeKind == LifeKind.WILDLIFE) wild++ else other++
                }
                assertTrue(wild > other, "$biome seed $seed: the wild outnumbers the settlement ($wild wild vs $other)")
            }
        }
    }

    @Test fun `a brave hunter preys on sapient adults - a timid one keeps the old line`() {
        val defs = GameConfig().enemies
        val wolf = defs.getValue("fang_wolf")          // default bravery 1f — brave
        val apex = defs.getValue("forest_apex")
        val shaman = defs.getValue("spore_shaman")     // a sapient adult of the camp
        assertEquals(LifeKind.SAPIENT, shaman.lifeKind)
        assertTrue(wolf.bravery >= Predation.BRAVE && apex.bravery >= Predation.BRAVE)
        assertTrue(Predation.canPredate(wolf, shaman), "a brave predator crosses the camp line")
        assertTrue(Predation.canPredate(apex, shaman), "an apex crosses it too")
        assertFalse(Predation.canPredate(wolf.copy(bravery = 0.2f), shaman),
            "a timid hunter still only threatens sapients")
        assertFalse(Predation.canPredate(defs.getValue("horn_deer"), shaman), "a grazer never hunts")
    }
}
