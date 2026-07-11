package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.161 細かい残り: a PROTECT request must be completable — the biome has to field the prey. */
class ProtectQuestEcologyTest {
    @Test fun `every biome that can ask PROTECT fields at least the quest's maximum count`() {
        // PlanetQuest asks for 1..2 predators and guards LONELY (and stormy skies) already —
        // every other biome must actually place ≥2 PREDATOR-role hunters under a clear sky,
        // or the request could be handed out and never fulfilled.
        val defs = GameConfig().enemies
        for (b in PlanetBiome.entries) {
            if (b == PlanetBiome.LONELY) continue
            for (seed in 1L..8L) {
                val n = SurfaceEcology.populate(b, 1000f, 1000f, 3400f, 2200f, Rng(seed))
                    .placements.count { defs[it.key]?.wildRole == WildRole.PREDATOR }
                assertTrue(n >= 2, "$b seed=$seed fields only $n predators — PROTECT asks up to 2")
            }
        }
    }
}
