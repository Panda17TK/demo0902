package io.github.panda17tk.arpg.ui

import io.github.panda17tk.arpg.save.Achievement
import kotlin.math.min

/**
 * v2.64 タイトル「記録」— the keeper's service record, read from the front door: the deepest
 * real run, the training ledger, and the achievement list (locked ones stay a quiet ？？？).
 * Pure text + geometry; the title screen draws it.
 */
object RecordsPanel {
    const val REPLAY_LABEL = "起動診断をもう一度"
    const val CLOSE_LABEL = "閉じる"

    /** The panel body, top-down. [has] answers whether an achievement is unlocked. */
    fun lines(
        bestWave: Int, bestKills: Int,
        simWave: Int, simKills: Int,
        has: (Achievement) -> Boolean,
    ): List<String> = buildList {
        add("── 到達記録 ──")
        add(
            if (bestWave > 0) "最深 同期汚染 $bestWave　総撃破 $bestKills"
            else "まだ記録がない — 星々はこれからあなたを知る"
        )
        add("── 訓練記録（旧式） ──")
        add(if (simWave > 0) "ウェーブ $simWave　撃破 $simKills" else "未実施")
        val unlocked = Achievement.entries.count(has)
        add("── 実績 $unlocked/${Achievement.entries.size} ──")
        for (a in Achievement.entries) {
            add(if (has(a)) "『${a.title}』 ${a.desc}" else "？？？")
        }
    }

    /** The two actions at the panel's foot: replay the boot diagnostic, and close. */
    fun buttons(w: Float, h: Float): List<UiButton> {
        val bw = min(300f, w * 0.66f)
        val x = (w - bw) / 2f
        return listOf(
            UiButton(x, h * 0.13f + 54f, bw, 44f, REPLAY_LABEL),
            UiButton(x, h * 0.13f, bw, 44f, CLOSE_LABEL),
        )
    }
}
