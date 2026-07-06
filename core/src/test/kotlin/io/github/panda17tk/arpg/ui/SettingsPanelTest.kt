package io.github.panda17tk.arpg.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.66 設定パネル: the five switches + close — labels, hints, and clean geometry. */
class SettingsPanelTest {
    @Test fun `the panel carries every switch plus close, in a stable order`() {
        val bs = SettingsPanel.buttons(360f, 780f)
        assertEquals(
            listOf(
                SettingsPanel.SOUND, SettingsPanel.VOLUME, SettingsPanel.HAPTICS, SettingsPanel.LEFTY,
                SettingsPanel.CONTROL_HINTS, SettingsPanel.LORE_HINTS,
                SettingsPanel.SHAKE, SettingsPanel.SOFT_FLASH, SettingsPanel.CLOSE_LABEL, // v2.96
            ),
            bs.map { it.label },
        )
    }

    @Test fun `every toggle explains itself in one quiet line`() {
        for (label in SettingsPanel.TOGGLES) {
            assertTrue(SettingsPanel.hintFor(label).isNotBlank(), "no hint for $label")
        }
    }

    @Test fun `rows fit on screen and never overlap`() {
        for ((w, h) in listOf(320f to 640f, 360f to 780f, 420f to 900f)) {
            val bs = SettingsPanel.buttons(w, h)
            for (b in bs) assertTrue(b.x >= 0f && b.y >= 0f && b.x + b.w <= w && b.y + b.h <= h, "off screen: $b at $w x $h")
            for (i in bs.indices) for (j in i + 1 until bs.size) {
                val a = bs[i]; val b = bs[j]
                val disjoint = a.y + a.h <= b.y || b.y + b.h <= a.y
                assertTrue(disjoint, "overlap $a / $b at $w x $h")
            }
        }
    }

    @Test fun `the settings chip mirrors the records chip and clears the menu`() {
        for ((w, h) in listOf(320f to 640f, 360f to 780f, 420f to 900f)) {
            val chip = TitleLayout.settingsButton(w, h)
            val rec = TitleLayout.recordsButton(w, h)
            assertTrue(chip.x >= 0f && chip.x + chip.w <= w && chip.y + chip.h <= h)
            assertTrue(chip.x + chip.w <= rec.x, "chips must not overlap at $w x $h")
            for (other in TitleLayout.buttons(w, h, hasSave = true)) {
                val disjoint = chip.x + chip.w <= other.x || other.x + other.w <= chip.x ||
                    chip.y + chip.h <= other.y || other.y + other.h <= chip.y
                assertTrue(disjoint, "chip overlaps $other at $w x $h")
            }
        }
    }
}
