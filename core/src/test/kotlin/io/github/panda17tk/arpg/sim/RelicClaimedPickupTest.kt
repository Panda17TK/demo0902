package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The relic claim's observable effect. PickupSystem.applyMaterial sets society.relicClaimed when a biome
 * material is collected; here we verify what that flag does to the surface goal (and the apex reward chain).
 */
class RelicClaimedPickupTest {
    @Test fun `claiming the relic advances the objective to the escape pad`() {
        val s = PlanetSocietyState().also { it.leaderDefeated = true }
        val before = SurfaceObjective.hudLine(PlanetBiome.NATURE, 0, s) // "…素材を回収せよ"
        s.relicClaimed = true
        val after = SurfaceObjective.hudLine(PlanetBiome.NATURE, 0, s)  // "…脱出パッドへ戻れ"
        assertNotEquals(before, after)
        assertTrue(after.contains("戻れ"), "claimed relic should point home: $after")
    }

    @Test fun `an unclaimed relic keeps the objective on collecting the material`() {
        val s = PlanetSocietyState().also { it.leaderDefeated = true }
        assertTrue(SurfaceObjective.hudLine(PlanetBiome.NATURE, 0, s).contains("素材"))
    }
}
