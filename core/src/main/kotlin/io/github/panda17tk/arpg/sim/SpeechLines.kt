package io.github.panda17tk.arpg.sim

/** Data-driven short lines intelligent creatures say, chosen by behavioural trigger. Pure. */
object SpeechLines {
    enum class Trigger {
        Warn, Aggro, Flee, Beg, Hide, Rest,
        ProtectChild, ProtectKing, Rally, KingEncounter, LonelyEncounter, Surrender,
    }

    private val LINES = mapOf(
        Trigger.Warn to listOf("近づくな", "ここは我らの土地だ", "それ以上来るな", "巣に近づくな"),
        Trigger.Aggro to listOf("警告はした", "敵だ！", "守れ！"),
        Trigger.Flee to listOf("勝てない…！", "退け！", "王に知らせろ！", "こいつは危険だ！"),
        Trigger.Beg to listOf("待て、殺すな", "降参する", "もう戦えない", "見逃してくれ"),
        Trigger.Hide to listOf("やり過ごす…", "傷を癒やせ", "まだ終わっていない"),
        Trigger.Rest to listOf("息を整えろ", "傷が塞がるまで…"),
        Trigger.ProtectChild to listOf("子らには触れさせない", "後ろへ下がれ", "この子だけは守る", "弱きものを狙うな！"),
        Trigger.ProtectKing to listOf("王に近づくな", "盾となれ", "我らが王を守れ"),
        Trigger.Rally to listOf("恐れるな！", "今こそ立て！", "この星を守れ！"),
        Trigger.KingEncounter to listOf("我が星から去れ", "小さき漂流者よ", "ここはお前の狩場ではない", "我らにも暮らしがある"),
        Trigger.LonelyEncounter to listOf("まだ誰かいたのか", "久しぶりの客だ", "撃つなら早くしろ", "話す気はあるか"),
        Trigger.Surrender to listOf("武器を置く", "もう争わない", "命だけは"),
    )

    /** Deterministic pick within the trigger's line set (salt varies the choice). Null if no lines. */
    fun pick(trigger: Trigger, salt: Int): String? {
        val l = LINES[trigger] ?: return null
        if (l.isEmpty()) return null
        return l[((salt % l.size) + l.size) % l.size]
    }

    /** Which line set fits a behavioural state (null = the creature stays silent in that state). */
    fun forState(state: CreatureState): Trigger? = when (state) {
        CreatureState.Warn -> Trigger.Warn
        CreatureState.Hostile -> Trigger.Aggro
        CreatureState.Flee -> Trigger.Flee
        CreatureState.Beg -> Trigger.Beg
        CreatureState.Hide -> Trigger.Hide
        CreatureState.Rest -> Trigger.Rest
        CreatureState.Protect -> Trigger.ProtectChild
        CreatureState.Rally -> Trigger.Rally
        CreatureState.Surrender -> Trigger.Surrender
        CreatureState.Ignore -> null
    }
}
