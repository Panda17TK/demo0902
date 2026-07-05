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

    /** The five toggle rows stacked under the panel head, then 閉じる at the foot.
     *  v2.84: rows grew to hold their two text lines (56dp + real gaps) and start below
     *  the 設定 header zone — the header used to print straight over the first row. */
    fun buttons(w: Float, h: Float): List<UiButton> {
        val bw = min(340f, w * 0.80f)
        val x = (w - bw) / 2f
        val top = h * 0.88f - 58f // the panel head above this belongs to the 設定 header
        val rows = TOGGLES.mapIndexed { i, label ->
            UiButton(x, top - 56f - i * 68f, bw, 56f, label)
        }
        return rows + UiButton(x, h * 0.13f, bw, 48f, CLOSE_LABEL)
    }
}
