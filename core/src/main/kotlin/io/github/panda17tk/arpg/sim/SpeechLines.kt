package io.github.panda17tk.arpg.sim

/** Data-driven short lines intelligent creatures say, chosen by behavioural trigger. Pure. */
object SpeechLines {
    enum class Trigger { Warn, Flee, Beg, Hide }

    private val LINES = mapOf(
        Trigger.Warn to listOf("近づくな", "ここは我らの土地だ", "それ以上来るな"),
        Trigger.Flee to listOf("退け！", "勝てない…", "逃げろ！"),
        Trigger.Beg to listOf("待て、殺すな", "降参する", "もう戦えない"),
        Trigger.Hide to listOf("どこへ行った…？", "やり過ごす"),
    )

    /** Deterministic pick within the trigger's line set (salt varies the choice). Null if no lines. */
    fun pick(trigger: Trigger, salt: Int): String? {
        val l = LINES[trigger] ?: return null
        if (l.isEmpty()) return null
        return l[((salt % l.size) + l.size) % l.size]
    }

    /** Which line set fits a behavioural state (null = the creature stays silent in that state). */
    fun forState(state: CreatureState): Trigger? = when (state) {
        CreatureState.Beg -> Trigger.Beg
        CreatureState.Flee -> Trigger.Flee
        CreatureState.Hide -> Trigger.Hide
        else -> null
    }
}
