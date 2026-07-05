package io.github.panda17tk.arpg.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.58 タイトル画面: menu geometry — a save adds つづきから on top; everything fits on screen. */
class TitleLayoutTest {
    private val sizes = listOf(320f to 640f, 360f to 780f, 420f to 900f)

    @Test fun `a saved run adds つづきから on top`() {
        val with = TitleLayout.buttons(360f, 780f, hasSave = true)
        val without = TitleLayout.buttons(360f, 780f, hasSave = false)
        assertEquals(listOf("つづきから", "はじめから", "旧式戦闘訓練"), with.map { it.label })
        assertEquals(listOf("はじめから", "旧式戦闘訓練"), without.map { it.label })
    }

    @Test fun `buttons stay on screen and never overlap`() {
        for ((w, h) in sizes) for (hasSave in listOf(true, false)) {
            val bs = TitleLayout.buttons(w, h, hasSave)
            for (b in bs) {
                assertTrue(b.x >= 0f && b.y >= 0f && b.x + b.w <= w && b.y + b.h <= h, "off screen: $b at $w x $h")
            }
            for (i in bs.indices) for (j in i + 1 until bs.size) {
                val a = bs[i]; val b = bs[j]
                val disjoint = a.x + a.w <= b.x || b.x + b.w <= a.x || a.y + a.h <= b.y || b.y + b.h <= a.y
                assertTrue(disjoint, "overlap $a / $b")
            }
        }
    }
}
