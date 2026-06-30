package io.github.panda17tk.arpg.sim

/**
 * The short line a remembered planet greets a returning player with. 星が覚えるなら、プレイヤーにもそれが
 * 伝わらなければならない。 Picks hostile vs indebted from the society's dominant feeling
 * ([SocietySpeechLines.returnGreeting]), then flavours it by the specific deed. Null when the memory is faint.
 * Pure → unit-testable; the single source of truth for the HUD's return-visit greeting (SurfaceObjective reuses it).
 */
object ReturnVisitLine {
    fun hudLine(society: PlanetSocietyState): String? = when (SocietySpeechLines.returnGreeting(society)) {
        SocietySpeechTrigger.ReturnVisitHostile ->
            if (society.childHarmed) "森は前の傷を覚えている" else "この星はあなたを敵として覚えている"
        SocietySpeechTrigger.ReturnVisitMerciful ->
            if (society.predatorKilledNearChild) "守護者はまだ借りを覚えている" else "この星はあなたへの借りを覚えている"
        else -> null
    }
}
