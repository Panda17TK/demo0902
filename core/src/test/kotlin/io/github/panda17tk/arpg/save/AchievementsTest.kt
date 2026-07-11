package io.github.panda17tk.arpg.save

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.62 実績: unlock-once semantics + logbook lines. State-agnostic: never assumes a clean
 *  slate, because a real Preferences backend may exist and persist across runs. */
class AchievementsTest {
    @Test fun `unlock fires exactly once`() {
        Achievements.unlock(Achievement.FIRST_JUMP) // idempotent warm-up (may or may not be new)
        assertTrue(Achievements.has(Achievement.FIRST_JUMP))
        assertFalse(Achievements.unlock(Achievement.FIRST_JUMP), "second unlock must be silent")
    }

    @Test fun `the tally counts unlocked over total`() {
        Achievements.unlock(Achievement.FIRST_LANDING)
        Achievements.unlock(Achievement.FIRST_HONE)
        assertTrue(Achievements.count() >= 2)
        assertTrue(Achievements.count() <= Achievements.total())
        assertEquals(Achievement.entries.size, Achievements.total())
    }

    @Test fun `logbook lines carry the tally and the unlocked titles`() {
        Achievements.unlock(Achievement.BOUNTY_HUNTER)
        val lines = Achievements.logLines()
        assertEquals("実績 ${Achievements.count()}/${Achievements.total()}", lines.first())
        assertTrue(lines.any { it.contains("賞金稼ぎ") })
        assertEquals(Achievements.count() + 1, lines.size) // tally line + one line per unlock
    }

    @Test fun `the badge counts only what the record screen has not shown`() {
        // v2.73: markSeen() zeroes the badge; a fresh unlock raises it by exactly one.
        Achievements.unlock(Achievement.FIRST_LANDING)
        Achievements.markSeen()
        assertEquals(0, Achievements.unseenCount())
        val fresh = Achievement.entries.firstOrNull { !Achievements.has(it) }
        if (fresh != null) {
            Achievements.unlock(fresh)
            assertEquals(1, Achievements.unseenCount())
            Achievements.markSeen()
            assertEquals(0, Achievements.unseenCount())
        }
    }

    @Test fun `the v117 records keep their stable ids at the tail`() {
        // v2.117: append-only — the ids are the save format, so they live at the end, verbatim.
        assertEquals(39, Achievement.entries.size) // v2.158 図鑑の二段: +保守記録の完成
        assertEquals("星の帳簿", Achievement.TRADE_LEDGER.title)
        assertEquals("図鑑の厚み", Achievement.BESTIARY_50.title)
        assertEquals("全記録", Achievement.BESTIARY_FULL.title)
        // v2.158: append-only — the new combat-book milestone takes the tail; FULL keeps its ordinal
        assertTrue(Achievement.BESTIARY_FULL.ordinal == Achievement.entries.size - 2)
        assertTrue(Achievement.BESTIARY_COMBAT.ordinal == Achievement.entries.size - 1)
        assertEquals("保守記録の完成", Achievement.BESTIARY_COMBAT.title)
    }

    @Test fun `every achievement has a calm title and description`() {
        for (a in Achievement.entries) {
            assertTrue(a.title.isNotBlank() && a.desc.isNotBlank())
        }
    }
}
