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
        assertTrue(fresh.any { it == "実績 0/${Achievement.entries.size}" })

        val seasoned = RecordsPanel.lines(12, 340, 7, 90) { true }
        assertTrue(seasoned.any { it.contains("最深 同期汚染 12") && it.contains("総撃破 340") })
        assertTrue(seasoned.any { it.contains("ウェーブ 7") && it.contains("撃破 90") })
        assertTrue(seasoned.any { it == "実績 ${Achievement.entries.size}/${Achievement.entries.size}" })
    }

    @Test fun `locked achievements are unspoken question marks in order, across the spreads`() {
        // v2.124: the titles moved off page 1 onto their own paged spread
        val only = setOf(Achievement.FIRST_LANDING, Achievement.BOUNTY_HUNTER)
        val pages = RecordsPanel.achievementPages()
        assertTrue(pages >= 2, "38 titles need more than one spread")
        val block = (0 until pages).flatMap { RecordsPanel.achievementLines(it) { a -> a in only }.drop(1) }
        val expected = Achievement.entries
            .map { if (it in only) "『${it.title}』" else "？？？" }
            .chunked(2)
            .map { it.joinToString("　") }
        assertEquals(expected, block, "every title appears exactly once, enum order kept")
        assertTrue(RecordsPanel.achievementLines(0) { false }.first().startsWith("実績 0/${Achievement.entries.size}（1/"))
        assertTrue(RecordsPanel.achievementLines(0) { false }.size <= 1 + RecordsPanel.ACH_ROWS)
    }

    @Test fun `the summary page fits one screen and names both books`() {
        // v2.124: page 1 had crept onto its own footer — it is a summary now, hard-capped
        val lines = RecordsPanel.lines(12, 340, 7, 90, clears = 2, chWeek = 2947, chWave = 9, chKills = 77,
            chDaysLeft = 3, stClock = "3:07", stKills = 210, stSorties = 9, bestiaryKnown = 41) { true }
        assertTrue(lines.size <= 13, "the summary stays short (got ${lines.size}: $lines)")
        assertTrue(lines.any { it == "実績 ${Achievement.entries.size}/${Achievement.entries.size}" })
        assertTrue(lines.any { it.startsWith("討伐図鑑 41/") })
        assertTrue(lines.none { it.contains("『") }, "titles live on their own spread now")
    }

    @Test fun `the footer actions fit on screen and never overlap, on both pages`() {
        for ((w, h) in listOf(320f to 640f, 360f to 780f, 420f to 900f)) {
            val bs = RecordsPanel.buttons(w, h)
            assertEquals( // v2.124: two columns keep the footer low
                listOf(RecordsPanel.BESTIARY_LABEL, RecordsPanel.ACHIEVEMENTS_LABEL, RecordsPanel.HANDOVER_LABEL, RecordsPanel.REPLAY_LABEL, RecordsPanel.CLOSE_LABEL),
                bs.map { it.label },
            )
            val page2 = RecordsPanel.buttons(w, h, bestiary = true)
            assertEquals(listOf("前へ", "次へ", RecordsPanel.BACK_LABEL, RecordsPanel.CLOSE_LABEL), page2.map { it.label }) // v2.120
            for (b in bs + page2) assertTrue(b.x >= 0f && b.y >= 0f && b.x + b.w <= w && b.y + b.h <= h, "off screen: $b")
            for (list in listOf(bs, page2)) for (i in list.indices) for (j in i + 1 until list.size) {
                val a = list[i]; val b = list[j]
                val disjoint = a.y + a.h <= b.y || b.y + b.h <= a.y || a.x + a.w <= b.x || b.x + b.w <= a.x
                assertTrue(disjoint, "overlap $a / $b") // v2.120: the pager pair shares a row
            }
        }
    }

    @Test fun `the bestiary names only what has fallen, six to a row, one spread at a time`() {
        val total = io.github.panda17tk.arpg.config.GameConfig().enemies.size
        val pages = RecordsPanel.bestiaryPages(total)
        val blank = RecordsPanel.bestiaryLines({ 0 })
        assertTrue(blank.first() == "討伐図鑑 0/$total（1/$pages）", "got ${blank.first()}")
        assertTrue(RecordsPanel.isHeader(blank.first()))
        assertTrue(blank.drop(1).all { line -> line.split("　").all { it == "？？？" } }, "unmet kinds stay unspoken")
        val hunter = RecordsPanel.bestiaryLines({ if (it == "zombie") 12 else 0 })
        assertTrue(hunter.first() == "討伐図鑑 1/$total（1/$pages）")
        val everySpread = (0 until pages).flatMap { RecordsPanel.bestiaryLines({ if (it == "zombie") 12 else 0 }, it) }
        assertTrue(everySpread.any { it.contains("ゾンビ×12") }, "a fallen kind shows its name and tally on its spread")
        // v2.120: a spread holds at most the header + BESTIARY_ROWS rows, and the pages tile the book
        assertTrue(blank.size <= 1 + RecordsPanel.BESTIARY_ROWS, "got ${blank.size}")
        val allRows = (0 until pages).sumOf { RecordsPanel.bestiaryLines({ 0 }, it).size - 1 }
        assertEquals((total + 5) / 6, allRows, "every kind appears on exactly one spread")
        assertTrue(pages >= 2, "124 kinds cannot pretend to be one page")
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
