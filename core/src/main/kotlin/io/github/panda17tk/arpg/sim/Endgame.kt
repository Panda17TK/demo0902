package io.github.panda17tk.arpg.sim

/**
 * v2.93 エンディング — the story finally closes. SyncRestoration tops out at 99 because the
 * last percent belonged to the people who could read the specifications; when the keeper
 * carries the network that far, the control core surfaces beside the gate and asks whether
 * they will take that last percent on. Pure rules + the final dialogue text.
 */
object Endgame {
    /** The network as whole as one keeper can make it — the core surfaces here. */
    const val THRESHOLD = 99

    fun ready(syncPercent: Int): Boolean = syncPercent >= THRESHOLD

    /** Where the control core rests, just off the gate's shoulder. */
    fun corePos(gate: Pair<Float, Float>): Pair<Float, Float> = gate.first + 150f to gate.second - 90f

    /** The final dialogue, page by page (tap advances; the last page carries the choice). */
    val PAGES: List<List<String>> = listOf(
        listOf(
            "署名照合、完了。……最終保守員、応答を確認。",
            "外部同期 99%。残る 1% は、仕様を書いた者たちの領分だった。",
        ),
        listOf(
            "彼らはもういない。だが網は、あなたの手で編み直された。",
            "星々は覚えている。守られた子らを。討たれた王を。返された遺物を。",
        ),
        listOf(
            "選んでほしい。",
            "最後の 1% を引き受け、網とともに眠りにつくか——",
            "それとも切断し、このまま漂い続けるか。",
        ),
    )

    const val CHOICE_SLEEP = "同期を完了して、眠りにつく"
    const val CHOICE_DRIFT = "切断して、漂流を続ける"

    /** The quiet epilogue after completing the sync (then the title). */
    val EPILOGUE: List<String> = listOf(
        "同期完了。全系、静かに稼働。",
        "人類保全装置群は、次の朝を待つ姿勢に戻る。",
        "星々は、あなたを覚えている。",
        "——最終保守員の記録、ここに閉じる。",
    )

    /** The one line the network leaves when the keeper chooses to keep drifting. */
    const val DRIFT_LINE = "切断を記録した。網は、それでもあなたを覚えている。"
}
