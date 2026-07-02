package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import kotlin.math.hypot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SurfaceEcologyTweaksTest {
    private val w = 3200f; private val h = 3200f
    private val sx = 1600f; private val sy = 1600f

    private fun populate(tweaks: SpawnTweaks, biome: PlanetBiome = PlanetBiome.NATURE, seed: Long = 7L) =
        SurfaceEcology.populate(biome, sx, sy, w, h, Rng(seed), PlanetContext.NEUTRAL, tweaks)

    @Test fun `neutral tweaks reproduce the exact legacy layout`() {
        for (biome in PlanetBiome.values()) {
            val legacy = SurfaceEcology.populate(biome, sx, sy, w, h, Rng(7L), PlanetContext.NEUTRAL)
            val neutral = populate(SpawnTweaks.NEUTRAL, biome)
            assertEquals(legacy, neutral, "NEUTRAL must not change the $biome layout")
        }
    }

    @Test fun `a thinned world has fewer grazers but never zero per group`() {
        val base = populate(SpawnTweaks.NEUTRAL)
        val thinned = populate(SpawnTweaks(herbivoreMul = 0.6f))
        val deer = { s: Society -> s.placements.count { it.key == "horn_deer" } }
        assertEquals(4, deer(base))
        assertEquals(2, deer(thinned)) // 4 × 0.6 = 2.4 → floor 2
        val hoppers = thinned.placements.count { it.key == "moss_hopper" }
        assertEquals(3, hoppers) // 5 × 0.6 = 3.0
        assertTrue(thinned.placements.any { it.key in setOf("horn_deer", "moss_hopper") }, "groups never vanish")
    }

    @Test fun `watch-guards stand within four tiles of the pad`() {
        val watched = populate(SpawnTweaks(extraGuardsAtPad = 2))
        val guards = watched.placements.takeLast(2)
        assertEquals(listOf("forest_guardian", "forest_guardian"), guards.map { it.key })
        for (g in guards) {
            val d = hypot(g.x - sx, g.y - sy)
            assertTrue(d <= Tuning.TILE * 4f + 0.001f, "guard too far from the pad: $d")
        }
        assertEquals(populate(SpawnTweaks.NEUTRAL).placements.size + 2, watched.placements.size)
    }
}
