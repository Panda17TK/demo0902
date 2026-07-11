package io.github.panda17tk.arpg.save

import io.github.panda17tk.arpg.sim.PlanetSocietyState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** v2.151 土台の堅牢化: every society field survives the DTO round trip with a DISTINCT value —
 *  a positional mis-mapping (field inserted mid-list) can no longer pass unnoticed. */
class MemoryRoundTripTest {
    @Test fun `a fully distinct society state survives the codec round trip field by field`() {
        val s = PlanetSocietyState(
            childHarmed = true, childKilled = false,
            wildPredatorThreatenedChild = true, predatorKilledNearChild = false,
            hatchlingKilled = true, nestMotherKilled = false,
            apexKilled = true, surrenderKilled = 3, surrenderedSpared = 5,
            leaderDefeated = true, relicClaimed = false,
            hostility = 0.61f, mercy = 0.23f, ecologicalDisruption = 0.47f,
        )
        val back = PlanetMemoryCodec.stateOf(PlanetMemoryCodec.dtoOf(s))
        assertEquals(true, back.childHarmed)
        assertEquals(false, back.childKilled)
        assertEquals(true, back.wildPredatorThreatenedChild)
        assertEquals(false, back.predatorKilledNearChild)
        assertEquals(true, back.hatchlingKilled)
        assertEquals(false, back.nestMotherKilled)
        assertEquals(true, back.apexKilled)
        assertEquals(3, back.surrenderKilled)
        assertEquals(5, back.surrenderedSpared)
        assertEquals(true, back.leaderDefeated)
        assertEquals(false, back.relicClaimed)
        assertEquals(0.61f, back.hostility)
        assertEquals(0.23f, back.mercy)
        assertEquals(0.47f, back.ecologicalDisruption)
        // copyState mirrors the same field list — keep it honest with the same distinct values
        val copy = s.copyState()
        assertEquals(0.61f, copy.hostility)
        assertEquals(5, copy.surrenderedSpared)
        assertEquals(true, copy.hatchlingKilled)
    }
}
