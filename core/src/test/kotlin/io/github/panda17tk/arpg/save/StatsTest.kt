package io.github.panda17tk.arpg.save

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.123 勤続記録: buffered time lands at the seams, kills sum from the world's tally,
 *  and the counters never throw without a storage backend. State-agnostic like Scores. */
class StatsTest {
    @Test fun `time buffers on tick and lands whole at the fold`() {
        val before = Stats.playSeconds
        repeat(180) { Stats.tick(1f / 60f) } // three seconds of frames
        assertEquals(before, Stats.playSeconds, "ticks alone touch nothing")
        Stats.fold(emptyMap())
        // float frames sum to 3s−ε; whole seconds land, the fraction carries to the next seam
        assertTrue(Stats.playSeconds >= before + 2, "the buffered seconds landed (got ${Stats.playSeconds - before})")
    }

    @Test fun `kills sum from the world's tally and the counters move one at a time`() {
        val kills = Stats.kills; val sorties = Stats.sorties; val deaths = Stats.deaths
        Stats.fold(mapOf("zombie" to 3, "wisp" to 2))
        assertEquals(kills + 5, Stats.kills)
        Stats.addSortie(); Stats.addDeath()
        assertEquals(sorties + 1, Stats.sorties)
        assertEquals(deaths + 1, Stats.deaths)
    }

    @Test fun `the clock reads as hours and zero-padded minutes`() {
        // the format contract, not the live value
        assertTrue(Regex("\\d+:\\d{2}").matches(Stats.clock()), "got ${Stats.clock()}")
    }

    @Test fun `the records line carries the service section`() {
        val lines = io.github.panda17tk.arpg.ui.RecordsPanel.lines(0, 0, 0, 0, stClock = "3:07", stKills = 210, stSorties = 9) { false }
        assertTrue(lines.any { it == "勤続記録" })
        assertTrue(lines.any { it.contains("累計 3:07") && it.contains("総撃破 210") && it.contains("出撃 9") }, "got $lines")
        assertTrue(io.github.panda17tk.arpg.ui.RecordsPanel.isHeader("勤続記録"))
        val fresh = io.github.panda17tk.arpg.ui.RecordsPanel.lines(0, 0, 0, 0) { false }
        assertTrue(fresh.any { it.startsWith("これから") }, "an empty record waits calmly")
    }
}
