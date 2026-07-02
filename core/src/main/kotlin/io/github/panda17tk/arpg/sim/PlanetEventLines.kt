package io.github.panda17tk.arpg.sim

/**
 * A change the society just recorded, in severity order (matches SocietySpeechLines.triggerFor):
 * the gravest deed speaks first when several land in the same tick.
 */
enum class SocietyDelta {
    CHILD_KILLED, APEX_KILLED, NEST_MOTHER_KILLED, HATCHLING_KILLED, CHILD_HARMED,
    PREDATOR_REPELLED, RELIC_CLAIMED, LEADER_DEFEATED, HOSTILITY_CROSSED, MERCY_CROSSED,
}

/**
 * The words of the surface event feed (LP v2.24) — the single source of these strings (§14.3).
 * Exhaustive when (no else) so a new delta is a compile error until it gets a sentence. Deeds
 * against what the planet holds sacred read one register harsher (vocabulary shared with
 * SurfaceObjective). Pure.
 */
object PlanetEventLines {
    fun line(delta: SocietyDelta, ctx: PlanetContext): PlanetEvent = when (delta) {
        SocietyDelta.CHILD_KILLED -> PlanetEvent(
            if (ctx.sacredThing == SacredThing.CHILDREN) "聖なる子が殺された　星は許さない" else "子が殺された　星は忘れない",
            EventKind.HOSTILE,
        )
        SocietyDelta.APEX_KILLED -> PlanetEvent(
            if (ctx.sacredThing == SacredThing.APEX) "神獣が倒れた　星の均衡が崩れる" else "星の主が沈んだ　均衡が揺らぐ",
            EventKind.ECOLOGY,
        )
        SocietyDelta.NEST_MOTHER_KILLED -> PlanetEvent("巣の守り手が倒れた", EventKind.ECOLOGY)
        SocietyDelta.HATCHLING_KILLED -> PlanetEvent("巣の子が失われた", EventKind.ECOLOGY)
        SocietyDelta.CHILD_HARMED -> PlanetEvent(
            if (ctx.sacredThing == SacredThing.CHILDREN) "子らを傷つけた　星は怒っている" else "弱きものを傷つけた　守護者が奮い立つ",
            EventKind.HOSTILE,
        )
        SocietyDelta.PREDATOR_REPELLED -> PlanetEvent("捕食者を退けた　森は見ていた", EventKind.MERCY)
        SocietyDelta.RELIC_CLAIMED -> PlanetEvent(
            if (ctx.sacredThing == SacredThing.RELIC) "聖なる遺物を持ち出した　星は怒っている" else "遺物を持ち出した",
            EventKind.HOSTILE,
        )
        SocietyDelta.LEADER_DEFEATED -> PlanetEvent("主を倒した　素材を回収せよ", EventKind.NEUTRAL)
        SocietyDelta.HOSTILITY_CROSSED -> PlanetEvent("この星はあなたを敵と見なした", EventKind.HOSTILE)
        SocietyDelta.MERCY_CROSSED -> PlanetEvent("この星はあなたに借りを感じている", EventKind.MERCY)
    }
}
