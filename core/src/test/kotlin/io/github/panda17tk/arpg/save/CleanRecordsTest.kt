package io.github.panda17tk.arpg.save

import io.github.panda17tk.arpg.sim.WorldState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/** v2.150 記録の清潔: the lifetime kill tally ignores wildlife, and each CORE stage earns its own visit. */
class CleanRecordsTest {
    @Test fun `the lifetime tally counts combat kills, not the fished-out sky`() {
        val before = Stats.kills
        Stats.fold(mapOf("star_sardine" to 300, "void_aji" to 200, "brute" to 3), setOf("star_sardine", "void_aji"))
        assertEquals(before + 3, Stats.kills, "500 fish stay out of the 勤続 tally")
    }

    @Test fun `settling a quest stage re-bases every counter and lowers the CORE flag`() {
        val ws = WorldState()
        ws.questKills = 7; ws.questElites = 2; ws.questDust = 40
        ws.questPredators = 3; ws.questTime = 12.5f; ws.coreVisited = true
        ws.snapshotQuestBases()
        assertEquals(7, ws.questBaseKills)
        assertEquals(2, ws.questBaseElites)
        assertEquals(40, ws.questBaseDust)
        assertEquals(3, ws.questBasePredators)
        assertEquals(12.5f, ws.questBaseTime)
        // v2.150: a second CORE request in the same chain demands its own walk to the core
        assertFalse(ws.coreVisited, "the visit flag drops at the stage seam")
    }
}
