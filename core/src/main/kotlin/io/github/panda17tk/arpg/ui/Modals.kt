package io.github.panda17tk.arpg.ui

import kotlin.math.min

/**
 * Pure modal/overlay geometry. From the HUD size (dp) it produces the tappable rects for each
 * blocking surface (upgrade intermission, game over, pause, help). Drawing and hit-testing both
 * read these same rects — geometry is pure and kept separate from rendering, the same policy
 * TouchLayout follows. Tap areas target ≥ ~56dp where reachable (spec §5.4).
 */
object Modals {
    const val CARD_H = 96f
    const val CARD_GAP = 16f
    const val BTN_H = 64f
    const val BTN_GAP = 16f
    const val PAUSE_BTN = 44f
    const val MARGIN = 12f

    /** Upgrade cards (intermission): [count] cards stacked, index 0 on top, horizontally centered. */
    fun upgradeCards(hudW: Float, hudH: Float, count: Int, labels: List<String> = emptyList()): List<UiButton> {
        if (count <= 0) return emptyList()
        val cardW = min(360f, hudW * 0.86f)
        val totalH = count * CARD_H + (count - 1) * CARD_GAP
        val x = (hudW - cardW) / 2f
        val top = (hudH + totalH) / 2f - CARD_H // bottom-left y of the first (top) card
        return (0 until count).map { i ->
            UiButton(x, top - i * (CARD_H + CARD_GAP), cardW, CARD_H, labels.getOrElse(i) { "" })
        }
    }

    /** Game over: a single centered "再挑戦" button sitting below the result text. */
    fun gameOverButtons(hudW: Float, hudH: Float): List<UiButton> {
        val btnW = min(360f, hudW * 0.5f).coerceAtLeast(160f)
        val x = (hudW - btnW) / 2f
        val y = hudH * 0.5f - 120f
        return listOf(UiButton(x, y, btnW, BTN_H, "再挑戦"))
    }

    /** The small ⏸ button shown in the top-right corner during play. */
    fun pauseButton(hudW: Float, hudH: Float): UiButton =
        UiButton(hudW - PAUSE_BTN - MARGIN, hudH - PAUSE_BTN - MARGIN, PAUSE_BTN, PAUSE_BTN, "II")

    /** Pause overlay: 再開 / 最初からやり直す / 操作説明 (+ この星の記憶 on a surface), stacked and centered. */
    fun pauseButtons(hudW: Float, hudH: Float, includeMemory: Boolean = false): List<UiButton> =
        stack(
            hudW, hudH,
            if (includeMemory) listOf("再開", "最初からやり直す", "操作説明", "この星の記憶", "宇宙の記憶を消す")
            else listOf("再開", "最初からやり直す", "操作説明", "宇宙の記憶を消す"),
        )

    /** LP v2.28: the 2-step confirmation for 「宇宙の記憶を消す」 — [消す][戻る], stacked and centered. */
    fun forgetButtons(hudW: Float, hudH: Float): List<UiButton> =
        stack(hudW, hudH, listOf("消す", "戻る"))

    /** Help overlay: one 戻る button near the bottom-center. */
    fun helpButtons(hudW: Float, hudH: Float): List<UiButton> {
        val btnW = min(320f, hudW * 0.5f).coerceAtLeast(140f)
        val x = (hudW - btnW) / 2f
        val y = hudH * 0.12f
        return listOf(UiButton(x, y, btnW, BTN_H, "戻る"))
    }

    private fun stack(hudW: Float, hudH: Float, labels: List<String>): List<UiButton> {
        val n = labels.size
        val btnW = min(320f, hudW * 0.7f)
        val totalH = n * BTN_H + (n - 1) * BTN_GAP
        val x = (hudW - btnW) / 2f
        val top = (hudH + totalH) / 2f - BTN_H
        return labels.mapIndexed { i, lab -> UiButton(x, top - i * (BTN_H + BTN_GAP), btnW, BTN_H, lab) }
    }

    /** Tap point → index of the first rect that contains it, or null if none do. */
    fun hitModal(rects: List<UiButton>, x: Float, y: Float): Int? {
        for (i in rects.indices) if (rects[i].contains(x, y)) return i
        return null
    }
}
