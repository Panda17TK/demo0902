package io.github.panda17tk.arpg.ui

import kotlin.math.min

/** v2.58 タイトル画面: pure geometry — the menu buttons, centered under the logo block. */
object TitleLayout {
    const val BTN_H = 54f
    const val GAP = 14f

    /** Menu buttons, top-down: つづきから (only with a saved run) / はじめから / 旧式戦闘訓練. */
    fun buttons(w: Float, h: Float, hasSave: Boolean): List<UiButton> {
        val labels = buildList {
            if (hasSave) add("つづきから")
            add("はじめから")
            add("旧式戦闘訓練")
        }
        val bw = min(320f, w * 0.72f)
        val top = h * 0.46f
        return labels.mapIndexed { i, lab ->
            UiButton((w - bw) / 2f, top - i * (BTN_H + GAP), bw, BTN_H, lab)
        }
    }

    /** v2.64 記録: a quiet corner chip, top-right — the service record lives behind it. */
    fun recordsButton(w: Float, h: Float): UiButton = UiButton(w - 104f, h - 52f, 92f, 40f, "記録")

    /** v2.59 設定: the sound / haptics toggle pair under the menu (labels drawn by the screen). */
    fun toggles(w: Float, h: Float): List<UiButton> {
        val bw = (min(320f, w * 0.72f) - GAP) / 2f
        val x = (w - bw * 2f - GAP) / 2f
        val y = h * 0.46f - 3f * (BTN_H + GAP) - 34f
        return listOf(
            UiButton(x, y, bw, 40f, "サウンド"),
            UiButton(x + bw + GAP, y, bw, 40f, "振動"),
        )
    }
}
