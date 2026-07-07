package io.github.panda17tk.arpg.ui

import kotlin.math.min

/**
 * v2.100 行商船 — the trading overlay's pure geometry: one plate per good (name left,
 * price right), a footer line for the dust balance, and [離れる] to close. GameScreen
 * draws and hit-tests against these same rects, like every other pure panel.
 */
object TraderPanel {
    const val CLOSE = "離れる"

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
}
