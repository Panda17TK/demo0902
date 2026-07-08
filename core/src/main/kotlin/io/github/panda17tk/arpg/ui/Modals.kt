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
        val y = hudH * 0.5f - 150f
        return listOf(
            UiButton(x, y, btnW, BTN_H, "もう一度"),
            UiButton(x, y - BTN_H - 12f, btnW, BTN_H, "タイトルへ"), // v2.59
        )
    }

    /** The small ⏸ button shown in the top-right corner during play. */
    fun pauseButton(hudW: Float, hudH: Float): UiButton =
        UiButton(hudW - PAUSE_BTN - MARGIN, hudH - PAUSE_BTN - MARGIN, PAUSE_BTN, PAUSE_BTN, "II")

    /** Pause overlay: 再開 / 最初からやり直す / 操作説明 (+ この星の記憶 on a surface), stacked and centered. */
    fun pauseButtons(hudW: Float, hudH: Float, includeMemory: Boolean = false, simActive: Boolean = false, challengeActive: Boolean = false): List<UiButton> =
        stack(
            hudW, hudH,
            buildList {
                add("再開"); add("最初からやり直す"); add("操作説明")
                if (includeMemory) add("この星の記憶")
                // v2.53: the old wave sim, walled off. v2.112: a proving run names itself honestly.
                add(if (challengeActive) "検証ランを終了" else if (simActive) "訓練を終了" else "旧式戦闘訓練")
                add("タイトルへ") // v2.58: auto-saves the run on the way out
                add("宇宙の記憶を消す")
            },
        )

    /** v2.60 起動診断: [診断する][スキップ] side by side, mid-screen. */
    fun tutorialBootButtons(hudW: Float, hudH: Float): List<UiButton> {
        val bw = min(240f, hudW * 0.42f)
        val gap = 12f
        val x = (hudW - bw * 2f - gap) / 2f
        val y = hudH * 0.5f - BTN_H / 2f
        return listOf(
            UiButton(x, y, bw, BTN_H, "診断する"),
            UiButton(x + bw + gap, y, bw, BTN_H, "スキップ"),
        )
    }

    /** v2.60 起動診断: the always-available small skip button under the prompt panel. */
    fun tutorialSkipButton(hudW: Float, hudH: Float): UiButton =
        UiButton(hudW - 132f, hudH - 244f, 120f, 40f, "スキップ")

    /** v2.56 ボタン配置エディタ: [大きく][小さく][リセット][完了] in one row.
     *  v2.84: dropped below the editor's hint panel (top ≈ hudH−138, two lines ≈ 66dp tall)
     *  — the row used to sit at hudH−190, straight under the panel's text. */
    fun layoutEditButtons(hudW: Float, hudH: Float): List<UiButton> {
        val labels = listOf("大きく", "小さく", "リセット", "完了")
        val gap = 8f
        val bw = (hudW - 2f * MARGIN - gap * (labels.size - 1)) / labels.size
        val y = hudH - 262f
        return labels.mapIndexed { i, lab -> UiButton(MARGIN + i * (bw + gap), y, bw, 48f, lab) }
    }

    /** v2.93 エンディング: the final choice — sleep with the network, or keep drifting. */
    fun endingButtons(hudW: Float, hudH: Float): List<UiButton> = stack(
        hudW, hudH,
        listOf(
            io.github.panda17tk.arpg.sim.Endgame.CHOICE_SLEEP,
            io.github.panda17tk.arpg.sim.Endgame.CHOICE_DRIFT,
        ),
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
