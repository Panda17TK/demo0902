package io.github.panda17tk.arpg.ui

import io.github.panda17tk.arpg.save.WorkshopCatalog
import kotlin.math.min

/**
 * v2.90 保守員の工房 — the title-screen shop's geometry: one row per catalog line (labelled by
 * item id; the screen draws title/rank/cost), then 閉じる at the foot. Same glass grid as 設定.
 */
object WorkshopPanel {
    const val CLOSE_LABEL = "閉じる"

    fun buttons(w: Float, h: Float): List<UiButton> {
        val bw = min(340f, w * 0.80f)
        val x = (w - bw) / 2f
        val top = h * 0.88f - 84f // the head above holds 工房 + the fragment bank line
        val rows = WorkshopCatalog.ITEMS.mapIndexed { i, item ->
            UiButton(x, top - 50f - i * 58f, bw, 50f, item.id) // v2.104: six crafts fit a small screen
        }
        return rows + UiButton(x, h * 0.13f, bw, 48f, CLOSE_LABEL)
    }
}
