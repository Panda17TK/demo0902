package io.github.panda17tk.arpg.sim

/**
 * v2.93 エンディング — the story finally closes. SyncRestoration tops out at 99 because the
 * last percent belonged to the people who could read the specifications; when the keeper
 * carries the network that far, the control core surfaces beside the gate and asks whether
 * they will take that last percent on. Pure rules + the final dialogue text.
 */
object Endgame {
    /** The network near-whole — the core surfaces here (v2.155: 99→90; most runs never crossed
     *  99, so the story's close went unseen. The cap stays 99 — the missing percent remains). */
    const val THRESHOLD = 90

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

    // v2.185 第3の結末: a gentle path that appears on the choice page ONLY to a keeper who
    // wronged no young anywhere and earned the trust of several worlds — instead of sleeping or
    // drifting, they unbind the net and let the pseudo-personas wake into a real morning.
    const val CHOICE_UNBIND = "網を解いて、朝を返す"

    /** True when this run's record is clean and trusted enough for the third door to open. */
    fun gentlePathOpen(memories: Collection<PlanetSocietyState>): Boolean {
        if (memories.isEmpty()) return false
        val noYoungHarmed = memories.none { it.childHarmed || it.childKilled || it.hatchlingKilled }
        val trusted = memories.count { it.mercy >= 0.5f }
        return noYoungHarmed && trusted >= 3
    }

    /** The epilogue after unbinding the net — a waking, not a sleep (then the title). */
    val EPILOGUE_UNBIND: List<String> = listOf(
        "同期を、完了ではなく解放に振り向けた。",
        "眠り続けるための網ではなく、目覚めるための朝へ。",
        "疑似人格たちは、初めて挨拶を返された朝を迎える。",
        "星々は、あなたを覚えている——赦した者として。",
        "——最終保守員の記録、朝へ閉じる。",
    )

    // v2.189 もう一つの結末: the dark mirror of the gentle path — for a keeper the stars remember
    // as the Star-Eater (young slain, worlds turned against them). It opens IN PLACE OF the gentle
    // door: the two conditions are exclusive (one demands no young harmed, the other demands blood).
    const val CHOICE_RECKON = "刻まれた名を受け入れる"

    /** True when the record is bloodied enough that the fourth door opens instead of the gentle one. */
    fun bloodPathOpen(memories: Collection<PlanetSocietyState>): Boolean {
        if (memories.isEmpty()) return false
        val slewYoung = memories.any { it.childKilled }
        val turned = memories.count { it.hostility >= 0.5f }
        return slewYoung && turned >= 3
    }

    /** The epilogue after accepting the name — the net does not forgive, but it will not forget. */
    val EPILOGUE_RECKON: List<String> = listOf(
        "同期は完了した。だが網が最後に綴じたのは、あなたの名だった。",
        "星喰い——守られるはずだった子らを、手にかけた者。",
        "赦しはない。それでも星々は、あなたを覚えている。",
        "忘却APIは、この網には無い。",
        "——最終保守員の記録、名とともに閉じる。",
    )
}
