package io.github.panda17tk.arpg.sim

/**
 * The short line a remembered planet greets a returning player with. 星が覚えるなら、プレイヤーにもそれが
 * 伝わらなければならない。 Picks hostile vs indebted from the society's dominant feeling
 * ([SocietySpeechLines.returnGreeting]), then flavours it by the specific deed. Null when the memory is faint.
 * Pure → unit-testable; the single source of truth for the HUD's return-visit greeting (the scan card reuses it).
 * v2.30 (10d): each deed carries a few variations — [salt] picks deterministically (the planet id keeps one
 * planet's voice stable); salt 0 is always the original sentence, so unsalted callers read exactly as before.
 */
object ReturnVisitLine {
    fun hudLine(society: PlanetSocietyState, salt: Int = 0): String? = when (SocietySpeechLines.returnGreeting(society)) {
        SocietySpeechTrigger.ReturnVisitHostile ->
            pick(if (society.childHarmed) HOSTILE_CHILD else HOSTILE_GENERIC, salt)
        SocietySpeechTrigger.ReturnVisitMerciful ->
            pick(if (society.predatorKilledNearChild) MERCY_GUARDIAN else MERCY_GENERIC, salt)
        else -> null
    }

    private fun pick(lines: List<String>, salt: Int): String = lines[((salt % lines.size) + lines.size) % lines.size]

    // Index 0 of each family is the legacy sentence — unsalted callers keep the exact old wording.
    private val HOSTILE_CHILD = listOf(
        "森は前の傷を覚えている",
        "子らはあなたの足音を知っている",
        "守護者たちがあなたの名を呟いている",
    )
    private val HOSTILE_GENERIC = listOf(
        "この星はあなたを敵として覚えている",
        "星があなたの帰還をざわめいている",
        "前に流した血を　星は忘れていない",
    )
    private val MERCY_GUARDIAN = listOf(
        "守護者はまだ借りを覚えている",
        "あの日守られた子が　あなたを見上げている",
        "守護者たちが道を空けている",
    )
    private val MERCY_GENERIC = listOf(
        "この星はあなたへの借りを覚えている",
        "星があなたの帰還を歓迎している",
        "民があなたの名を良い方に覚えている",
    )
}
