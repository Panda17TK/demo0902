package io.github.panda17tk.arpg.ui

/**
 * v2.46 航海日誌 — pure line builder for the inventory's 記録 tab. The run's present tense
 * (system/wave/kills/purse) up top, the all-time bests, then what the stars remember of you.
 */
object Logbook {
    fun lines(
        system: Int, wave: Int, kills: Int, dust: Int, shards: Int,
        bestWave: Int, bestKills: Int,
        planetLines: List<String>,
    ): List<String> {
        val head = listOf(
            "第${system}星系　ウェーブ $wave",
            "今回の撃破 $kills　星屑 $dust　ゲート鍵 $shards",
            "自己ベスト　ウェーブ $bestWave　撃破 $bestKills",
            "",
            "── 星の記憶 ──",
        )
        return head + planetLines.ifEmpty { listOf("まだどの星にも知られていない") }
    }
}
