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
        // v2.49 段階開示: the logbook's own header quietly tracks what the network now knows.
        val title = when {
            system >= 4 -> "肩書 最終保守員（照合済み）"
            system >= 2 -> "肩書 漂流者（署名照合中）"
            else -> "肩書 漂流者（未照合）"
        }
        val head = listOf(
            "第${system}星系　ウェーブ $wave",
            title,
            "今回の撃破 $kills　星屑 $dust　ゲート鍵 $shards",
            "自己ベスト　ウェーブ $bestWave　撃破 $bestKills",
            "",
            "── 星の記憶 ──",
        )
        return head + planetLines.ifEmpty { listOf("まだどの星にも知られていない") }
    }
}
