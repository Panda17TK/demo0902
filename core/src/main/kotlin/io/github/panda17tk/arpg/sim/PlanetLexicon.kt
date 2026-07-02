package io.github.panda17tk.arpg.sim

/**
 * The display dictionary for a planet's character (Living Planets v2.23) — the Japanese words the
 * scan card uses for temperament / sacred thing / story-seed omen. Single source of truth for these
 * strings (spec §14.3): GameScreen / Hud never hard-code them. Every enum value is covered by an
 * exhaustive when (no else) so adding a value is a compile error until it gets a word here.
 * The omen is a deliberately vague hint — never an instruction (goals are v2.26's job). Pure.
 */
object PlanetLexicon {
    fun temperament(t: PlanetTemperament): String = when (t) {
        PlanetTemperament.GENTLE -> "穏やか"
        PlanetTemperament.PROUD -> "誇り高い"
        PlanetTemperament.FEARFUL -> "臆病"
        PlanetTemperament.HUNGRY -> "飢えている"
        PlanetTemperament.RITUALISTIC -> "儀式的"
        PlanetTemperament.VENGEFUL -> "執念深い"
        PlanetTemperament.ANCIENT -> "太古"
        PlanetTemperament.SILENT -> "沈黙"
    }

    fun sacred(s: SacredThing): String = when (s) {
        SacredThing.KING -> "王"
        SacredThing.CHILDREN -> "子供たち"
        SacredThing.APEX -> "神獣"
        SacredThing.NEST -> "巣"
        SacredThing.RELIC -> "遺物"
        SacredThing.FIRE -> "火"
        SacredThing.ICE -> "氷"
        SacredThing.STORM -> "嵐"
        SacredThing.RUINS -> "遺跡"
        SacredThing.SILENCE -> "静寂"
    }

    /** The one-line character header 「気質　◯◯　　聖　◯◯」 (shared by the scan card and the memory summary). */
    fun traitLine(ctx: PlanetContext): String =
        "気質　${temperament(ctx.temperament)}　　聖　${sacred(ctx.sacredThing)}"

    /** A spoiler-free omen for the planet's story seed; null when the planet carries none. */
    fun omen(seed: PlanetStorySeed): String? = when (seed) {
        PlanetStorySeed.NONE -> null
        PlanetStorySeed.HUNGRY_FOREST -> "森が飢えている"
        PlanetStorySeed.LOST_CHILD -> "迷子の気配がする"
        PlanetStorySeed.APEX_WORSHIP -> "神獣が崇められている"
        PlanetStorySeed.PREDATOR_INVASION -> "捕食者が押し寄せている"
        PlanetStorySeed.BROKEN_SHRINE -> "祠が壊れている"
        PlanetStorySeed.EXILED_HEIR -> "追われた世継ぎがいる"
        PlanetStorySeed.SILENT_MONASTERY -> "沈黙の僧院がある"
        PlanetStorySeed.NESTING_SEASON -> "営巣の季節"
        PlanetStorySeed.RELIC_FAMINE -> "遺物が飢えを呼んでいる"
    }
}
