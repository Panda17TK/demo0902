package io.github.panda17tk.arpg.ui

import kotlin.math.min

/**
 * v2.103 セーブスロット — the title's slot picker: three journey plates and [閉じる].
 * Pure geometry; the title screen owns the summaries and draws/hit-tests these rects.
 */
object SlotPanel {
    const val CLOSE = "閉じる"

    fun rows(w: Float, h: Float): List<UiButton> {
        val bw = min(340f, w * 0.86f)
        val x = (w - bw) / 2f
        val top = h * 0.64f
        return (0 until io.github.panda17tk.arpg.save.SaveSlots.COUNT).map { i ->
            UiButton(x, top - 58f - i * 68f, bw, 58f, "")
        }
    }

    fun closeButton(w: Float, h: Float): UiButton {
        val bw = min(220f, w * 0.60f)
        return UiButton((w - bw) / 2f, h * 0.14f, bw, 46f, CLOSE)
    }
}
