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
        // v2.84: plain section headers — the ── flourishes were noise (isHeader colours them).
        add("到達記録")
        add(
            if (bestWave > 0) "最深 同期汚染 $bestWave　総撃破 $bestKills"
            else "まだ記録がない — 星々はこれからあなたを知る"
        )
        add("訓練記録（旧式）")
        add(if (simWave > 0) "ウェーブ $simWave　撃破 $simKills" else "未実施")
        val unlocked = Achievement.entries.count(has)
        add("実績 $unlocked/${Achievement.entries.size}")
        // v2.70: 18 entries — titles two to a row so the panel never outgrows a small screen
        // (the full description still arrives with the unlock toast and in the 勤務記録).
        Achievement.entries
            .map { if (has(it)) "『${it.title}』" else "？？？" }
            .chunked(2)
            .forEach { add(it.joinToString("　")) }
    }

    /** v2.84: which lines are section headers (drawn muted by the title screen). */
    fun isHeader(line: String): Boolean =
        line == "到達記録" || line == "訓練記録（旧式）" || line.startsWith("実績 ")

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
