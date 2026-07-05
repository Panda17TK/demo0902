package io.github.panda17tk.arpg.ui

import kotlin.math.min

/**
 * v2.66 設定パネル — the title's toggle pair had outgrown its corner, so every switch moved
 * into one glass panel behind a 設定 chip: sound, haptics, the mirrored layout, and the two
 * hint channels from the tutorial design (操作ヒント / 世界観ヒント). Pure geometry + labels.
 */
object SettingsPanel {
    const val SOUND = "サウンド"
    const val HAPTICS = "振動"
    const val LEFTY = "左利き配置"
    const val CONTROL_HINTS = "操作ヒント"
    const val LORE_HINTS = "世界観ヒント"
    const val CLOSE_LABEL = "閉じる"

    /** Toggle rows in display order — index-stable for the screen's state mapping. */
    val TOGGLES = listOf(SOUND, HAPTICS, LEFTY, CONTROL_HINTS, LORE_HINTS)

    /** One-line whispers under each toggle name (drawn smaller by the screen). */
    fun hintFor(label: String): String = when (label) {
        SOUND -> "効果音と環境音"
        HAPTICS -> "被弾などの振動"
        LEFTY -> "スティック右・ボタン左に反転"
        CONTROL_HINTS -> "着陸方法などの操作ガイド"
        LORE_HINTS -> "記憶核の語り・遭難記録"
        else -> ""
    }

    /** The five toggle rows stacked under the panel head, then 閉じる at the foot. */
    fun buttons(w: Float, h: Float): List<UiButton> {
        val bw = min(320f, w * 0.72f)
        val x = (w - bw) / 2f
        val top = h * 0.78f
        val rows = TOGGLES.mapIndexed { i, label ->
            UiButton(x, top - i * 54f, bw, 44f, label)
        }
        return rows + UiButton(x, h * 0.13f, bw, 44f, CLOSE_LABEL)
    }
}
