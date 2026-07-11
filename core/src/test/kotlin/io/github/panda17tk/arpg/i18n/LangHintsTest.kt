package io.github.panda17tk.arpg.i18n

import io.github.panda17tk.arpg.sim.TutorialController
import io.github.panda17tk.arpg.ui.Onboarding
import io.github.panda17tk.arpg.ui.SettingsPanel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.148 英語の穴埋め: the hint band speaks English — boot diagnostic, onboarding, space hints. */
class LangHintsTest {
    private fun assertEnglish(s: String) {
        val t = Lang.tr(s)
        assertTrue(t.none { it.code >= 0x2E80 && it != '　' }, "still CJK: 「$s」 -> 「$t」")
    }

    @Test fun `the boot diagnostic speaks English at every step`() {
        Lang.en = true
        try {
            for (touch in listOf(true, false)) {
                val t = TutorialController()
                t.prompt(touch).forEach(::assertEnglish) // BOOT_PROMPT
                t.begin()
                t.prompt(touch).forEach(::assertEnglish) // MOVE
                t.onMoved(999f); t.prompt(touch).forEach(::assertEnglish)
                t.onKill(); t.prompt(touch).forEach(::assertEnglish)
                t.onDustPicked(); t.prompt(touch).forEach(::assertEnglish)
                t.onDash(); t.prompt(touch).forEach(::assertEnglish)
                t.onScan(); t.prompt(touch).forEach(::assertEnglish)
                t.onLanded(); t.prompt(touch).forEach(::assertEnglish)
                t.onSurfaceTick(TutorialController.OBSERVE_TIME)
                t.prompt(touch).forEach(::assertEnglish) // RETURN_PAD
                assertEnglish(t.completionToast())
                t.skip(); assertEnglish(t.completionToast())
            }
        } finally { Lang.en = false }
    }

    @Test fun `onboarding and the space hints speak English`() {
        Lang.en = true
        try {
            for (touch in listOf(true, false)) {
                var s = 0f
                while (s < Onboarding.END) { Onboarding.lineFor(s, touch)?.let(::assertEnglish); s += 8f }
            }
            for (s in listOf(
                "訓練環境 — 模擬戦闘のみ", "惑星をタップで着陸", "惑星に近づいて [L] で着陸",
                "[L] 着陸", "タップで着陸", "[L] 離陸", "[L] 離陸して宇宙へ",
                "ゲート鍵 2/3", "ジャンプゲート → 420", "最寄りの惑星 ← 999",
            )) assertEnglish(s)
        } finally { Lang.en = false }
    }

    @Test fun `the haptics toggle and its whisper keep their own words`() {
        // v2.148: v2.145 shortened the whisper onto the toggle label's key — the later mapOf entry
        // won and the Haptics toggle read "Vibration". The two sources must stay distinct.
        assertTrue(SettingsPanel.HAPTICS != SettingsPanel.hintFor(SettingsPanel.HAPTICS))
        Lang.en = true
        try {
            assertEquals("Haptics", Lang.tr(SettingsPanel.HAPTICS))
            assertEquals("Vibration on hits", Lang.tr(SettingsPanel.hintFor(SettingsPanel.HAPTICS)))
        } finally { Lang.en = false }
    }
}
