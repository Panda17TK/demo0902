package io.github.panda17tk.arpg.sim

/**
 * v2.97 難易度 — three ways to run the same universe. Pure multipliers, injected into the
 * world like the workshop boons: the sim reads them, the title screen picks and persists.
 * 安定運転 softens the keeper's wounds and speeds the mend; 過負荷 deepens the surge but
 * every kill sheds an extra dust pile. 標準 is the universe as designed.
 */
enum class Difficulty(val label: String, val hint: String) {
    CALM("安定運転", "被ダメ0.7倍・休息回復1.5倍"),
    NORMAL("標準", "設計どおりの宙域"),
    OVERLOAD("過負荷", "物量1.3倍・星屑+1山");

    /** How much of incoming damage the keeper actually takes. */
    val dmgTakenMul: Float
        get() = when (this) {
            CALM -> 0.7f
            NORMAL -> 1f
            OVERLOAD -> 1f
        }

    /** Multiplies the rest-mend rate. */
    val regenMul: Float
        get() = when (this) {
            CALM -> 1.5f
            NORMAL -> 1f
            OVERLOAD -> 1f
        }

    /** Multiplies the surge's wave quota. */
    val quotaMul: Float
        get() = when (this) {
            CALM -> 1f
            NORMAL -> 1f
            OVERLOAD -> 1.3f
        }

    /** Extra dust piles a kill sheds. */
    val dustBonus: Int
        get() = if (this == OVERLOAD) 1 else 0

    /** The next mode in the title chip's cycle. */
    fun next(): Difficulty = entries[(ordinal + 1) % entries.size]

    companion object {
        fun byName(name: String?): Difficulty = entries.firstOrNull { it.name == name } ?: NORMAL
    }
}
