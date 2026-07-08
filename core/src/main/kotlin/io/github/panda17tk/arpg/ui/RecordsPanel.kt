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
    const val BESTIARY_LABEL = "討伐図鑑を見る" // v2.113
    const val BESTIARY_ROWS = 11 // v2.120: rows per bestiary page (six kinds to a row)
    const val BACK_LABEL = "記録へ戻る"
    const val HANDOVER_LABEL = "引き継ぎ" // v2.122: the account moves as one block of text
    const val EXPORT_LABEL = "書き出す（コピー）"
    const val IMPORT_LABEL = "取り込む（貼り付け）"

    /** The panel body, top-down. [has] answers whether an achievement is unlocked. */
    fun lines(
        bestWave: Int, bestKills: Int,
        simWave: Int, simKills: Int,
        clears: Int = 0, // v2.93: completed syncs
        chWeek: Long = 0L, // v2.102 検証ラン: this week's proving-run ledger
        chWave: Int = 0, chKills: Int = 0,
        chDaysLeft: Int = 0, // v2.119: how long this week's sky still stands
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
        add("検証ラン（今週の宙域）")
        val turn = if (chDaysLeft in 1..7) "　残り${chDaysLeft}日" else "" // v2.119
        add(if (chWave > 0) "W$chWeek　ウェーブ $chWave　撃破 $chKills$turn" else "未実施$turn")
        if (clears > 0) add("同期完了 ${clears}回 — 網は眠り、また編み直された") // v2.93
        val unlocked = Achievement.entries.count(has)
        add("実績 $unlocked/${Achievement.entries.size}")
        // v2.70: 18 entries — titles two to a row so the panel never outgrows a small screen
        // (the full description still arrives with the unlock toast and in the 勤務記録).
        Achievement.entries
            .map { if (has(it)) "『${it.title}』" else "？？？" }
            .chunked(2)
            .forEach { add(it.joinToString("　")) }
    }

    /** v2.113 討伐図鑑: the second page — every enemy kind, named once it has fallen to you.
     *  [count] answers kills for a kind id; unmet kinds stay a quiet ？？？. Four to a row. */
    fun bestiaryLines(count: (String) -> Int, page: Int = 0): List<String> {
        val kinds = io.github.panda17tk.arpg.config.GameConfig().enemies.entries.sortedBy { it.key }
        val known = kinds.count { count(it.key) > 0 }
        val rows = kinds.map { (id, def) -> if (count(id) > 0) "${def.name}×${count(id)}" else "？？？" }
            .chunked(6) // six to a row; v2.120 pages the rows so the glyphs stay readable
            .map { it.joinToString("　") }
        val pages = bestiaryPages(kinds.size)
        val p = page.coerceIn(0, pages - 1)
        return buildList {
            add("討伐図鑑 $known/${kinds.size}（${p + 1}/$pages）")
            addAll(rows.drop(p * BESTIARY_ROWS).take(BESTIARY_ROWS))
        }
    }

    /** v2.120: how many pages the book spans for [kindCount] kinds. */
    fun bestiaryPages(kindCount: Int): Int {
        val rows = (kindCount + 5) / 6
        return if (rows <= 0) 1 else (rows + BESTIARY_ROWS - 1) / BESTIARY_ROWS
    }

    /** v2.84: which lines are section headers (drawn muted by the title screen). */
    fun isHeader(line: String): Boolean =
        line == "到達記録" || line == "訓練記録（旧式）" || line == "検証ラン（今週の宙域）" ||
            line.startsWith("実績 ") || line.startsWith("討伐図鑑 ") || line == "引き継ぎ"

    /** The actions at the panel's foot. Page 1 (記録): 図鑑/診断/閉じる. Page 2 (図鑑): 戻る/閉じる. */
    fun buttons(w: Float, h: Float, bestiary: Boolean = false): List<UiButton> {
        val bw = min(300f, w * 0.66f)
        val x = (w - bw) / 2f
        return if (bestiary) { // v2.120: the pager pair rides above the exits
            val half = (bw - 8f) / 2f
            listOf(
                UiButton(x, h * 0.13f + 108f, half, 44f, "前へ"),
                UiButton(x + half + 8f, h * 0.13f + 108f, half, 44f, "次へ"),
                UiButton(x, h * 0.13f + 54f, bw, 44f, BACK_LABEL),
                UiButton(x, h * 0.13f, bw, 44f, CLOSE_LABEL),
            )
        } else listOf(
            UiButton(x, h * 0.13f + 162f, bw, 44f, HANDOVER_LABEL), // v2.122
            UiButton(x, h * 0.13f + 108f, bw, 44f, BESTIARY_LABEL), // v2.113
            UiButton(x, h * 0.13f + 54f, bw, 44f, REPLAY_LABEL),
            UiButton(x, h * 0.13f, bw, 44f, CLOSE_LABEL),
        )
    }

    /** v2.122 引き継ぎ: the page's calm explanation (first line is its header). */
    fun handoverLines(): List<String> = listOf(
        "引き継ぎ",
        "書き出すと、すべての記録と設定がひとつの引き継ぎ文になり",
        "クリップボードへ入る（メモ帳などに貼って持ち運べる）",
        "新しい端末で同じ文をコピーしてから取り込むと、記録がこの形に戻る",
        "取り込みは今の記録を上書きする——先に書き出しておくと安全",
    )

    fun handoverButtons(w: Float, h: Float): List<UiButton> {
        val bw = min(300f, w * 0.66f)
        val x = (w - bw) / 2f
        return listOf(
            UiButton(x, h * 0.13f + 162f, bw, 44f, EXPORT_LABEL),
            UiButton(x, h * 0.13f + 108f, bw, 44f, IMPORT_LABEL),
            UiButton(x, h * 0.13f + 54f, bw, 44f, BACK_LABEL),
            UiButton(x, h * 0.13f, bw, 44f, CLOSE_LABEL),
        )
    }
}
