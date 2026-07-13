package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.93 エンディング: the threshold, the core's rest, and the words that close the story. */
class EndgameTest {
    @Test fun `the core surfaces only when the sync tops out`() {
        assertFalse(Endgame.ready(89))
        assertTrue(Endgame.ready(90)) // v2.155 結末への道: the door opens where runs actually reach
        assertEquals(90, Endgame.THRESHOLD, "the cap stays 99 — the missing percent remains the story")
    }

    @Test fun `the core rests just off the gate's shoulder`() {
        val (cx, cy) = Endgame.corePos(1000f to 800f)
        assertEquals(1150f, cx)
        assertEquals(710f, cy)
    }

    @Test fun `the dialogue closes the story and both endings have words`() {
        assertTrue(Endgame.PAGES.size >= 3, "a real conversation, not a toast")
        for (page in Endgame.PAGES) assertTrue(page.isNotEmpty() && page.all { it.isNotBlank() })
        assertTrue(Endgame.PAGES.last().any { it.contains("選") }, "the last page carries the question")
        assertTrue(Endgame.CHOICE_SLEEP.isNotBlank() && Endgame.CHOICE_DRIFT.isNotBlank())
        assertTrue(Endgame.EPILOGUE.size >= 3, "the record closes with more than one breath")
        assertTrue(Endgame.EPILOGUE.last().contains("閉じる"))
        assertTrue(Endgame.DRIFT_LINE.isNotBlank())
        // v2.185 第3の結末: the earned close carries its own words too
        assertTrue(Endgame.CHOICE_UNBIND.isNotBlank())
        assertTrue(Endgame.EPILOGUE_UNBIND.size >= 3 && Endgame.EPILOGUE_UNBIND.last().contains("閉じる"))
    }

    @Test fun `the gentle path opens only to a clean, trusted record`() { // v2.185 第3の結末
        assertFalse(Endgame.gentlePathOpen(emptyList()), "no journey, no third door")
        val clean = List(4) { PlanetSocietyState().apply { mercy = 0.6f } }
        assertTrue(Endgame.gentlePathOpen(clean), "clean hands + trust opens it")
        val bloodied = List(4) { i -> PlanetSocietyState().apply { mercy = 0.6f; if (i == 0) childKilled = true } }
        assertFalse(Endgame.gentlePathOpen(bloodied), "one harmed child closes it")
        val untrusted = List(4) { PlanetSocietyState() } // no trust earned
        assertFalse(Endgame.gentlePathOpen(untrusted), "no trust, no door")
    }

    @Test fun `the sync arithmetic can actually reach the threshold`() {
        // 8 systems in + 8 planets visited + 5 trusted → (7*12) + 40 + 40 = 99+ (capped at 99)
        val states = List(8) { i -> PlanetSocietyState().apply { if (i < 5) mercy = 0.6f } }
        assertTrue(SyncRestoration.percent(8, states) >= Endgame.THRESHOLD, "the ending is reachable")
    }
}
