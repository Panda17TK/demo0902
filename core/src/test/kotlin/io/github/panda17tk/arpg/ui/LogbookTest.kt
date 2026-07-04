package io.github.panda17tk.arpg.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.46 航海日誌: the pure line builder for the 記録 tab. */
class LogbookTest {
    @Test fun `carries the run, the bests and the planet memories`() {
        val lines = Logbook.lines(
            system = 2, wave = 9, kills = 41, dust = 120, shards = 1,
            bestWave = 15, bestKills = 200,
            planetLines = listOf("星7　王を討った", "星9　子を守った"),
        )
        assertTrue(lines[0].contains("第2星系") && lines[0].contains("9"))
        assertTrue(lines[1].contains("肩書")) // v2.49: the header tracks the network's verdict
        assertTrue(lines[2].contains("41") && lines[2].contains("120") && lines[2].contains("1"))
        assertTrue(lines[3].contains("15") && lines[3].contains("200"))
        assertTrue(lines.any { it.contains("星の記憶") })
        assertTrue(lines.any { it.contains("星7") })
    }

    @Test fun `an unknown drifter gets the blank-book line`() {
        val lines = Logbook.lines(1, 1, 0, 0, 0, 1, 0, emptyList())
        assertEquals("まだどの星にも知られていない", lines.last())
    }

    @Test fun `the title follows the revelation stages`() {
        assertTrue(Logbook.lines(1, 1, 0, 0, 0, 1, 0, emptyList())[1].contains("未照合"))
        assertTrue(Logbook.lines(2, 1, 0, 0, 0, 1, 0, emptyList())[1].contains("照合中"))
        assertTrue(Logbook.lines(4, 1, 0, 0, 0, 1, 0, emptyList())[1].contains("最終保守員"))
    }

    @Test fun `the LOG tab exists in the inventory`() {
        assertEquals(6, InventoryLayout.TAB_LABELS.size)
        assertEquals("記録", InventoryLayout.TAB_LABELS.last())
        assertEquals(InvTab.LOG, InvTab.entries.last())
    }
}
