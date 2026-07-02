package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TakeoffRewardTest {
    private val neutral = PlanetContext.NEUTRAL

    @Test fun `an uneventful visit to an indifferent star earns nothing`() {
        val r = TakeoffReward.compute(PlanetSocietyState(), PlanetBiome.DEAD, neutral)
        assertTrue(r.isEmpty)
        assertNull(TakeoffReward.toastFor(r, childKilled = false))
    }

    @Test fun `felling the leader opens the stores`() {
        val r = TakeoffReward.compute(PlanetSocietyState(leaderDefeated = true), PlanetBiome.DEAD, neutral)
        assertEquals(TakeoffReward.LEADER_BLOCKS, r.blocks)
    }

    @Test fun `the relic tops up every ammo pool`() {
        val r = TakeoffReward.compute(PlanetSocietyState(relicClaimed = true), PlanetBiome.DEAD, neutral)
        assertEquals(TakeoffReward.RELIC_AMMO_PCT, r.ammoPct)
    }

    @Test fun `mercy sits exactly on its boundary`() {
        val below = TakeoffReward.compute(PlanetSocietyState(mercy = 0.49f), PlanetBiome.DEAD, neutral)
        assertEquals(0f, below.med)
        val at = TakeoffReward.compute(PlanetSocietyState(mercy = 0.5f), PlanetBiome.DEAD, neutral)
        assertEquals(TakeoffReward.MERCY_MED, at.med)
        assertEquals(TakeoffReward.MERCY_BLOCKS, at.blocks)
    }

    @Test fun `untouched children on a watching world earn gratitude`() {
        val r = TakeoffReward.compute(PlanetSocietyState(), PlanetBiome.NATURE, neutral)
        assertEquals(TakeoffReward.PROTECT_BLOCKS, r.blocks)
        val harmed = TakeoffReward.compute(PlanetSocietyState(childHarmed = true), PlanetBiome.NATURE, neutral)
        assertEquals(0, harmed.blocks)
    }

    @Test fun `a child-killer gets nothing no matter what else was done`() {
        val s = PlanetSocietyState(
            childKilled = true, leaderDefeated = true, relicClaimed = true, mercy = 1f,
        )
        val r = TakeoffReward.compute(s, PlanetBiome.NATURE, neutral)
        assertTrue(r.isEmpty)
        assertEquals(TakeoffReward.TOAST_NOTHING, TakeoffReward.toastFor(r, childKilled = true))
    }

    @Test fun `a earned bundle announces the send-off`() {
        val r = TakeoffReward.compute(PlanetSocietyState(leaderDefeated = true), PlanetBiome.DEAD, neutral)
        assertEquals(TakeoffReward.TOAST_GIFT, TakeoffReward.toastFor(r, childKilled = false))
    }
}
