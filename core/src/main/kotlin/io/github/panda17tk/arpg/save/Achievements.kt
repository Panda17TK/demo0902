package io.github.panda17tk.arpg.save

import com.badlogic.gdx.Gdx

/**
 * v2.62 実績: quiet milestones in the keeper's service record. Pure ids + names; unlocked ids
 * persist as a comma-joined string in Preferences (defensive like Scores — never throws).
 */
enum class Achievement(val title: String, val desc: String) {
    FIRST_LANDING("初着陸", "はじめて星に降りた"),
    FIRST_JUMP("初跳躍", "ジャンプゲートで次の星系へ渡った"),
    STAR_RETURNER("星還し", "ふたつの星から深い信頼を得た"),
    KING_SLAYER("王殺し", "ふたつの星で王を討った"),
    FIRST_HONE("鍛え直し", "武器をはじめて合成した"),
    SYNC_50("復旧の兆し", "星間同期復旧が50%に達した"),
    BOUNTY_HUNTER("賞金稼ぎ", "賞金首を討ち取った"),
    DEEP_SURGE("深層乱流", "同期汚染 15 を生き延びた"),
    // v2.68: four more entries in the service record (append-only — ids are the save format).
    QUEST_PATRON("依頼人の信頼", "星の依頼を果たして離陸した"),
    RELIC_KEEPER("遺物継承", "眠っていた遺物を持ち帰った"),
    SYNC_90("通信回復目前", "星間同期復旧が90%に達した"),
    HONED_MAX("鍛え上げ", "武器を+3まで研ぎ上げた"),
    // v2.70: six more — the record fills in as the keeper's habits show (append-only ids).
    GUARDIAN("守り手", "護衛の依頼を果たした"),
    OBSERVER("観測員", "定点観測の依頼を果たした"),
    SYSTEM_3("第三星系", "みっつ目の星系に到達した"),
    DUST_RICH("記憶片収集家", "星屑500を懐に抱えた"),
    QUIET_VISIT("静かな来訪", "誰も傷つけずに星を発った"),
    BEAST_HUNTER("獣狩り", "ふたつの星で獣の王を討った"),
}

object Achievements {
    private const val PREFS = "arpg-achievements"
    private const val KEY = "unlocked.v1"

    private val unlocked = mutableSetOf<Achievement>()

    fun load() {
        unlocked.clear()
        try {
            val text = Gdx.app.getPreferences(PREFS).getString(KEY, "")
            text.split(',').forEach { name ->
                Achievement.entries.firstOrNull { it.name == name }?.let { unlocked.add(it) }
            }
        } catch (_: Throwable) { /* keep in-memory */ }
    }

    fun has(a: Achievement): Boolean = a in unlocked
    fun count(): Int = unlocked.size
    fun total(): Int = Achievement.entries.size

    /** Unlock [a]; returns true only the FIRST time (the caller shows the toast then). */
    fun unlock(a: Achievement): Boolean {
        if (a in unlocked) return false
        unlocked.add(a)
        try {
            val p = Gdx.app.getPreferences(PREFS)
            p.putString(KEY, unlocked.joinToString(",") { it.name })
            p.flush()
        } catch (_: Throwable) { /* persist best-effort */ }
        return true
    }

    /** Logbook lines: the tally + each unlocked title (locked ones stay unspoken). */
    fun logLines(): List<String> {
        // NOTE: computed OUTSIDE buildList — inside it, count() resolves to MutableList.count().
        val tally = "── 実績 ${count()}/${total()} ──"
        return listOf(tally) + unlocked.sortedBy { it.ordinal }.map { "『${it.title}』 ${it.desc}" }
    }
}
