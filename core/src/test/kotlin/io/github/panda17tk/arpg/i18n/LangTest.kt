package io.github.panda17tk.arpg.i18n

import io.github.panda17tk.arpg.ui.InventoryLayout
import io.github.panda17tk.arpg.ui.Modals
import io.github.panda17tk.arpg.ui.RecordsPanel
import io.github.panda17tk.arpg.ui.SettingsPanel
import io.github.panda17tk.arpg.ui.TitleLayout
import io.github.panda17tk.arpg.ui.TraderPanel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.115 英語化第1弾: the dictionary is presentation-only — off means untouched, on means
 *  every main label speaks English, and anything unknown passes through unchanged. */
class LangTest {
    @AfterEach fun reset() { Lang.en = false } // Lang is global — leave it as other tests expect

    @Test fun `off means every string passes through untouched`() {
        Lang.en = false
        assertEquals("つづきから", Lang.tr("つづきから"))
        assertEquals("閉じる", Lang.tr("閉じる"))
    }

    @Test fun `every main label speaks English when asked`() {
        Lang.en = true
        val labels = buildList {
            addAll(TitleLayout.buttons(360f, 800f, hasSave = true).map { it.label })
            add(TitleLayout.recordsButton(360f, 800f).label)
            add(TitleLayout.settingsButton(360f, 800f).label)
            add(TitleLayout.workshopButton(360f, 800f).label)
            addAll(SettingsPanel.TOGGLES)
            addAll(SettingsPanel.TOGGLES.map { SettingsPanel.hintFor(it) })
            add(SettingsPanel.CLOSE_LABEL)
            addAll(InventoryLayout.TAB_LABELS)
            addAll(InventoryLayout.SLOT_LABELS)
            addAll(Modals.pauseButtons(360f, 800f, includeMemory = true).map { it.label })
            add("訓練を終了"); add("検証ランを終了") // the pause exit's other two moods
            addAll(listOf(TraderPanel.SELL, TraderPanel.BACK, TraderPanel.CLOSE))
            addAll(listOf(RecordsPanel.BESTIARY_LABEL, RecordsPanel.REPLAY_LABEL, RecordsPanel.BACK_LABEL, RecordsPanel.CLOSE_LABEL))
        }
        for (s in labels) {
            val t = Lang.tr(s)
            assertTrue(t != s, "no English for 「$s」")
            assertTrue(t.all { it.code < 0x2E80 }, "still CJK: 「$s」 -> 「$t」")
        }
    }

    @Test fun `composed lines translate token by token`() {
        // v2.121 第2弾: the HUD strip, slot lines, and sale notes are built at runtime —
        // the substring table carries them, longest token first.
        Lang.en = true
        assertEquals("Desync 4　processes 3　stability 62%", Lang.tr("同期汚染 4　残プロセス 3　宙域安定 62%"))
        assertEquals("Slot 1　empty", Lang.tr("スロット1　empty").let { Lang.tr("スロット1　空き") })
        assertEquals("Achievements 3/38", Lang.tr("実績 3/38"))
        // the full-width space (U+3000) is layout, not language — it may stay
        assertTrue(Lang.tr("W2947　ウェーブ 9　撃破 77　残り3日").none { it.code >= 0x2E80 && it != '　' })
    }

    @Test fun `every weapon, card, and achievement wears an English name`() {
        Lang.en = true
        for (w in io.github.panda17tk.arpg.combat.Weapons.ALL) {
            assertTrue(Lang.tr(w.name) != w.name, "no English for weapon 「${w.name}」")
        }
        for (u in io.github.panda17tk.arpg.upgrade.Upgrades.ALL) {
            assertTrue(Lang.tr(u.name) != u.name, "no English for card 「${u.name}」")
        }
        for (a in io.github.panda17tk.arpg.save.Achievement.entries) {
            assertTrue(Lang.tr(a.title) != a.title, "no English for achievement 「${a.title}」")
        }
    }

    @Test fun `lines outside every dictionary still pass through`() {
        Lang.en = true
        assertEquals("qqq 123", Lang.tr("qqq 123"))
    }
}
