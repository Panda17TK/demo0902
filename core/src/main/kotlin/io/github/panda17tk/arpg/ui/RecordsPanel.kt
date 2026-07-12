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
    const val ACHIEVEMENTS_LABEL = "実績を見る" // v2.124: the titles moved to their own spread
    const val ACH_ROWS = 11 // v2.124: achievement rows per spread (two titles to a row)
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
        dayKey: Long = 0L, dayWave: Int = 0, dayKills: Int = 0, // v2.180 今日の宙域
        dayIsToday: Boolean = false, // stale daily bests read as 未実施 — the sky has turned
        stClock: String = "", stKills: Long = 0, stSorties: Long = 0, // v2.123 勤続記録
        bestiaryKnown: Int = -1, // v2.124: the summary count (negative hides the line)
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
        add("検証ラン（今日の宙域）") // v2.180
        add(if (dayWave > 0 && dayIsToday) "D$dayKey　ウェーブ $dayWave　撃破 $dayKills" else "未実施 — 空は毎日あたらしい")
        if (clears > 0) add("同期完了 ${clears}回 — 網は眠り、また編み直された") // v2.93
        add("勤続記録") // v2.123
        add(if (stSorties > 0) "累計 $stClock　総撃破 $stKills　出撃 $stSorties" else "これから — 最初の出撃を待っている")
        val unlocked = Achievement.entries.count(has)
        // v2.124: page 1 is a summary — the title list and the bestiary live on their own spreads,
        // so this page never outgrows one screen again (it had crept onto the footer buttons).
        add("実績 $unlocked/${Achievement.entries.size}")
        if (bestiaryKnown >= 0) add("討伐図鑑 $bestiaryKnown/${kindCount()}")
    }

    /** v2.113 討伐図鑑: the second page — every enemy kind, named once it has fallen to you.
     *  [count] answers kills for a kind id; unmet kinds stay a quiet ？？？. Four to a row. */
    /** v2.124: the book needs only ids and display names — built once, GameConfig is heavy. */
    private val kinds: List<Pair<String, String>> by lazy {
        io.github.panda17tk.arpg.config.GameConfig().enemies.entries.sortedBy { it.key }.map { it.key to it.value.name }
    }

    fun kindCount(): Int = kinds.size

    // v2.161 細かい残り: the combat frame (every non-wildlife kind — v2.158's reachable
    // milestone) gets a live counter beside the full-book tally.
    private val combatIds: Set<String> by lazy {
        io.github.panda17tk.arpg.config.GameConfig().enemies
            .filterValues { it.lifeKind != io.github.panda17tk.arpg.config.LifeKind.WILDLIFE }.keys
    }

    fun bestiaryLines(count: (String) -> Int, page: Int = 0): List<String> {
        val known = kinds.count { count(it.first) > 0 }
        val rows = kinds.map { (id, name) -> if (count(id) > 0) "$name×${count(id)}" else "？？？" }
            .chunked(6) // six to a row; v2.120 pages the rows so the glyphs stay readable
            .map { it.joinToString("　") }
        val pages = bestiaryPages(kinds.size)
        val p = page.coerceIn(0, pages - 1)
        return buildList {
            val combatKnown = combatIds.count { count(it) > 0 } // v2.161
            add("討伐図鑑 $known/${kinds.size}　戦闘枠 $combatKnown/${combatIds.size}（${p + 1}/$pages）")
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
            line == "検証ラン（今日の宙域）" || // v2.180
            line.startsWith("実績 ") || line.startsWith("討伐図鑑 ") || line == "引き継ぎ" || line == "勤続記録"

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
        } else { // v2.124: two columns keep the footer low — the summary lines stay clear above
            val half = (bw - 8f) / 2f
            listOf(
                UiButton(x, h * 0.13f + 108f, half, 44f, BESTIARY_LABEL),
                UiButton(x + half + 8f, h * 0.13f + 108f, half, 44f, ACHIEVEMENTS_LABEL),
                UiButton(x, h * 0.13f + 54f, half, 44f, HANDOVER_LABEL),
                UiButton(x + half + 8f, h * 0.13f + 54f, half, 44f, REPLAY_LABEL),
                UiButton(x, h * 0.13f, bw, 44f, CLOSE_LABEL),
            )
        }
    }

    /** v2.122 引き継ぎ: the page's calm explanation (first line is its header). */
    fun handoverLines(): List<String> = listOf(
        "引き継ぎ",
        "書き出し: 全記録と設定をクリップボードの一文に",
        "取り込み: 同じ文を貼って復元 — 今の記録は上書き",
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

    /** v2.124 実績の見開き: the full title list, two to a row, paged like the bestiary. */
    fun achievementLines(page: Int = 0, has: (Achievement) -> Boolean): List<String> {
        val rows = Achievement.entries
            .map { if (has(it)) "『${it.title}』" else "？？？" }
            .chunked(2)
            .map { it.joinToString("　") }
        val pages = achievementPages()
        val p = page.coerceIn(0, pages - 1)
        val unlocked = Achievement.entries.count(has)
        return buildList {
            add("実績 $unlocked/${Achievement.entries.size}（${p + 1}/$pages）")
            addAll(rows.drop(p * ACH_ROWS).take(ACH_ROWS))
        }
    }

    fun achievementPages(): Int {
        val rows = (Achievement.entries.size + 1) / 2
        return if (rows <= 0) 1 else (rows + ACH_ROWS - 1) / ACH_ROWS
    }
}
