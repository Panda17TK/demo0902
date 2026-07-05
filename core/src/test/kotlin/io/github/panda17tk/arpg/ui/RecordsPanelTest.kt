package io.github.panda17tk.arpg.ui

import io.github.panda17tk.arpg.save.Achievement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.64 タイトル「記録」: the service-record lines and the panel's two actions. */
class RecordsPanelTest {
    @Test fun `records show when they exist and stay quiet when they do not`() {
        val fresh = RecordsPanel.lines(0, 0, 0, 0) { false }
        assertTrue(fresh.any { it.contains("まだ記録がない") })
        assertTrue(fresh.any { it == "未実施" })
        assertTrue(fresh.any { it == "── 実績 0/${Achievement.entries.size} ──" })

        val seasoned = RecordsPanel.lines(12, 340, 7, 90) { true }
        assertTrue(seasoned.any { it.contains("最深 同期汚染 12") && it.contains("総撃破 340") })
        assertTrue(seasoned.any { it.contains("ウェーブ 7") && it.contains("撃破 90") })
        assertTrue(seasoned.any { it == "── 実績 ${Achievement.entries.size}/${Achievement.entries.size} ──" })
    }

    @Test fun `locked achievements are unspoken question marks in order`() {
        val only = setOf(Achievement.FIRST_LANDING, Achievement.BOUNTY_HUNTER)
        val lines = RecordsPanel.lines(1, 1, 0, 0) { it in only }
        // v2.70: the block is the last ceil(n/2) lines — titles two to a row, enum order kept
        val rows = (Achievement.entries.size + 1) / 2
        val block = lines.takeLast(rows)
        val expected = Achievement.entries
            .map { if (it in only) "『${it.title}』" else "？？？" }
            .chunked(2)
            .map { it.joinToString("　") }
        assertEquals(expected, block)
        assertTrue(block.any { it.contains("初着陸") } && block.any { it.contains("賞金稼ぎ") })
    }

    @Test fun `the two actions fit on screen and never overlap`() {
        for ((w, h) in listOf(320f to 640f, 360f to 780f, 420f to 900f)) {
            val bs = RecordsPanel.buttons(w, h)
            assertEquals(listOf(RecordsPanel.REPLAY_LABEL, RecordsPanel.CLOSE_LABEL), bs.map { it.label })
            for (b in bs) assertTrue(b.x >= 0f && b.y >= 0f && b.x + b.w <= w && b.y + b.h <= h, "off screen: $b")
            val (a, b) = bs
            val disjoint = a.y + a.h <= b.y || b.y + b.h <= a.y
            assertTrue(disjoint, "overlap $a / $b")
        }
    }

    @Test fun `the records chip sits clear of the menu and the settings chip`() {
        for ((w, h) in listOf(320f to 640f, 360f to 780f, 420f to 900f)) {
            val chip = TitleLayout.recordsButton(w, h)
            assertTrue(chip.x >= 0f && chip.y >= 0f && chip.x + chip.w <= w && chip.y + chip.h <= h)
            for (other in TitleLayout.buttons(w, h, hasSave = true) + TitleLayout.settingsButton(w, h)) {
                val disjoint = chip.x + chip.w <= other.x || other.x + other.w <= chip.x ||
                    chip.y + chip.h <= other.y || other.y + other.h <= chip.y
                assertTrue(disjoint, "chip overlaps $other at $w x $h")
            }
        }
    }
}
