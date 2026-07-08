package io.github.panda17tk.arpg.ui

import kotlin.math.min

/**
 * v2.100 行商船 — the trading overlay's pure geometry: one plate per good (name left,
 * price right), a footer line for the dust balance, and [離れる] to close. GameScreen
 * draws and hit-tests against these same rects, like every other pure panel.
 */
object TraderPanel {
    const val CLOSE = "離れる"
    const val SELL = "売る"     // v2.114: flips the stall to buyback
    const val BACK = "棚へ戻る" // v2.114: back to the shelves
    const val SELL_ROWS = 6     // backpack items shown per page
    const val UNDO = "戻す"     // v2.118: buy the last sale back at the same price

    /** One tappable plate per stock slot, top-down under the title band. */
    fun rows(w: Float, h: Float, count: Int): List<UiButton> {
        val bw = min(360f, w * 0.90f)
        val x = (w - bw) / 2f
        val top = h * 0.72f
        return (0 until count).map { i -> UiButton(x, top - 52f - i * 62f, bw, 52f, "") }
    }

    fun closeButton(w: Float, h: Float): UiButton {
        val bw = min(220f, w * 0.60f)
        return UiButton((w - bw) / 2f, h * 0.115f, bw, 48f, CLOSE)
    }

    /** v2.114 買い取り: the shelf page's footer pair [売る][離れる], centered as one unit. */
    fun shelfFooter(w: Float, h: Float): List<UiButton> {
        val bw = min(300f, w * 0.84f)
        val x = (w - bw) / 2f
        val y = h * 0.115f
        val sellW = bw * 0.42f
        return listOf(
            UiButton(x, y, sellW, 48f, SELL),
            UiButton(x + sellW + 8f, y, bw - sellW - 8f, 48f, CLOSE),
        )
    }

    /** The buyback page's item plates (up to [SELL_ROWS] of the backpack per page). */
    fun sellRows(w: Float, h: Float, count: Int): List<UiButton> {
        val bw = min(360f, w * 0.90f)
        val x = (w - bw) / 2f
        val top = h * 0.76f
        return (0 until count.coerceAtMost(SELL_ROWS)).map { i ->
            UiButton(x, top - 48f - i * 58f, bw, 48f, "")
        }
    }

    /** The buyback page's footer: [前へ][次へ][棚へ戻る]. */
    fun sellFooter(w: Float, h: Float): List<UiButton> {
        val bw = min(360f, w * 0.90f)
        val x = (w - bw) / 2f
        val each = (bw - 16f) / 3f
        val y = h * 0.115f
        return listOf(
            UiButton(x, y, each, 48f, "前へ"),
            UiButton(x + each + 8f, y, each, 48f, "次へ"),
            UiButton(x + 2 * (each + 8f), y, each, 48f, BACK),
        )
    }

    /** v2.118: the undo chip sits below the buyback footer, clear of everything. */
    fun undoButton(w: Float, h: Float): UiButton {
        val bw = min(280f, w * 0.70f)
        return UiButton((w - bw) / 2f, h * 0.115f - 56f, bw, 44f, UNDO)
    }

    fun sellPages(count: Int): Int = if (count <= 0) 1 else (count + SELL_ROWS - 1) / SELL_ROWS
}
