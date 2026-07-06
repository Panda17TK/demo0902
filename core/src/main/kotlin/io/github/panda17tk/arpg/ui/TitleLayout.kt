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

    /** v2.66 設定: its twin, top-left — every toggle moved into the settings panel behind it. */
    fun settingsButton(w: Float, h: Float): UiButton = UiButton(12f, h - 52f, 92f, 40f, "設定")

    /** v2.90 工房: the keeper's workshop, docked under 設定 — permanent boons live behind it. */
    fun workshopButton(w: Float, h: Float): UiButton = UiButton(12f, h - 100f, 92f, 40f, "工房")

    /** v2.97 難易度: the run-mode chip under 工房 — taps cycle 安定運転/標準/過負荷. */
    fun difficultyButton(w: Float, h: Float, label: String): UiButton = UiButton(12f, h - 148f, 92f, 40f, label)
}
