package io.github.panda17tk.arpg.save

import io.github.panda17tk.arpg.ui.SlotPanel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.103 セーブスロット: three journeys, three keys — and the legacy save stays slot 1. */
class SaveSlotsTest {
    @Test fun `slot 0 keeps the legacy key so old saves survive the update`() {
        assertEquals("run.v1", SaveSlots.keyFor("run.v1", 0))
        assertEquals("book.v1", SaveSlots.keyFor("book.v1", 0))
    }

    @Test fun `every slot writes under its own key`() {
        val keys = (0 until SaveSlots.COUNT).map { SaveSlots.keyFor("run.v1", it) }
        assertEquals(SaveSlots.COUNT, keys.toSet().size, "no two journeys share a shelf: $keys")
        assertEquals("run.v1.s1", SaveSlots.keyFor("run.v1", 1))
        assertEquals("run.v1.s2", SaveSlots.keyFor("run.v1", 2))
    }

    @Test fun `the picker stacks three plates above the close button, all on screen`() {
        for ((w, h) in listOf(320f to 640f, 360f to 780f, 420f to 900f)) {
            val rows = SlotPanel.rows(w, h)
            assertEquals(SaveSlots.COUNT, rows.size)
            for (b in rows) assertTrue(b.x >= 0f && b.y >= 0f && b.x + b.w <= w && b.y + b.h <= h, "off screen: $b")
            for (i in 0 until rows.size - 1) assertTrue(rows[i].y > rows[i + 1].y, "plates stack top-down")
            val close = SlotPanel.closeButton(w, h)
            assertTrue(close.y + close.h < rows.last().y, "[閉じる] sits clear below the plates at $w x $h")
        }
    }
}
