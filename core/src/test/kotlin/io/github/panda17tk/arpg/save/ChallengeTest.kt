package io.github.panda17tk.arpg.save

import io.github.panda17tk.arpg.ui.RecordsPanel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.102 検証ラン: the weekly clock, the weekly sky, and the weekly ledger. */
class ChallengeTest {
    @org.junit.jupiter.api.Test fun `the week's remaining days count down to the Monday turn`() {
        // v2.119: 1970-01-05 was a Monday — a fresh week opens with 7 days on the clock.
        val monday = 4L * 86_400_000L
        org.junit.jupiter.api.Assertions.assertEquals(7, Challenge.daysLeft(monday))
        org.junit.jupiter.api.Assertions.assertEquals(1, Challenge.daysLeft(monday - 1L))
        org.junit.jupiter.api.Assertions.assertEquals(1, Challenge.daysLeft(monday + 6L * 86_400_000L))
        for (t in listOf(0L, monday, monday + 3L * 86_400_000L, 1_752_000_000_000L)) {
            org.junit.jupiter.api.Assertions.assertTrue(Challenge.daysLeft(t) in 1..7, "out of range at $t")
            // the day the clock hits zero is exactly the day the week index turns
            org.junit.jupiter.api.Assertions.assertEquals(
                Challenge.weekOf(t) + 1,
                Challenge.weekOf(t + Challenge.daysLeft(t).toLong() * 86_400_000L),
                "the sky turns when the clock runs out (at $t)",
            )
        }
    }

    @org.junit.jupiter.api.Test fun `the records line wears the deadline`() {
        val lines = io.github.panda17tk.arpg.ui.RecordsPanel.lines(0, 0, 0, 0, chWeek = 2947L, chWave = 9, chKills = 77, chDaysLeft = 3) { false }
        org.junit.jupiter.api.Assertions.assertTrue(lines.any { it.contains("残り3日") }, "got $lines")
    }

    private val day = 86_400_000L

    @Test fun `the week turns on Monday`() {
        // 1970-01-01 was a Thursday: Thu..Sun share week 0, Monday opens week 1.
        assertEquals(Challenge.weekOf(0L), Challenge.weekOf(3 * day + day - 1), "Thu..Sun, one sky")
        assertEquals(Challenge.weekOf(0L) + 1, Challenge.weekOf(4 * day), "Monday turns the sky")
        // any two instants inside the same week agree
        assertEquals(Challenge.weekOf(4 * day), Challenge.weekOf(10 * day + day - 1))
    }

    @Test fun `every week gets its own stable sky, and never the home system's seed`() {
        for (week in 0L..200L) {
            val seed = Challenge.seedFor(week)
            assertEquals(seed, Challenge.seedFor(week), "stable")
            assertTrue(seed > 1L, "seed 1 is the home system (and ≤1 means no trait): got $seed for week $week")
            assertNotEquals(seed, Challenge.seedFor(week + 1), "consecutive weeks differ")
        }
        assertEquals("W2947", Challenge.codeFor(2947L))
    }

    @Test fun `the challenge ledger keeps its own slot and wipes when the sky turns`() {
        Scores.recordChallenge(10L, 5, 20)
        assertEquals(10L, Scores.chWeek)
        assertEquals(5, Scores.chBestWave)
        assertEquals(20, Scores.chBestKills)
        assertTrue(!Scores.recordChallenge(10L, 3, 50), "a shallower run is no new best")
        assertEquals(5, Scores.chBestWave, "the best stands")
        assertEquals(50, Scores.chBestKills, "kills climb independently")
        assertTrue(Scores.recordChallenge(11L, 2, 2), "a new week starts blank — 2 beats nothing")
        assertEquals(11L, Scores.chWeek)
        assertEquals(2, Scores.chBestWave)
        assertEquals(2, Scores.chBestKills)
        // the real-run and training ledgers are untouched by any of this
        assertEquals(0, Scores.bestWave + Scores.simBestWave)
    }

    @Test fun `the records panel gives the proving run its own section`() {
        val fresh = RecordsPanel.lines(0, 0, 0, 0) { false }
        assertTrue(fresh.any { it == "検証ラン（今週の宙域）" })
        assertTrue(RecordsPanel.isHeader("検証ラン（今週の宙域）"))
        val run = RecordsPanel.lines(0, 0, 0, 0, chWeek = 2947L, chWave = 9, chKills = 77) { false }
        assertTrue(run.any { it.contains("W2947") && it.contains("ウェーブ 9") && it.contains("撃破 77") })
    }
}
