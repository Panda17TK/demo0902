package io.github.panda17tk.arpg.i18n

/**
 * v2.182 図録 — full English codex lines for the lore-bearing kinds. Keyed by the stable roster
 * id (ids are the save contract; the Japanese originals on EnemyDef.lore stay the source of
 * truth). The codex pane swaps the whole line when English UI is on — sentence-level, not token
 * substitution, mirroring LoreEn for the readables.
 */
object SpeciesLoreEn {
    fun textOf(id: String): String? = TEXTS[id]
    fun ids(): Set<String> = TEXTS.keys

    private val TEXTS: Map<String, String> = mapOf(
        "overlord" to "A king knotted from sync's fraying - with no one left to command.",
        "beast_king" to "The forest sleeps and wakes with its master; it never forgets who harms the children.",
        "forest_apex" to "Big as a boulder, its eyes quiet. Fire on it and the whole planet turns on you.",
        "frost_worm" to "The white worm beneath the ice - the ice folk feared it, and wove it into a lullaby.",
        "volcano_king" to "A king upon a molten throne; its wrath is the mountainside itself.",
        "ice_queen" to "She rules a freezing stillness - time stops in whoever she touches.",
        "storm_core" to "The shapeless storm's heart, raging on without a word.",
        "exiled_king" to "Driven from the throne, a ghost that still cannot lay down its crown.",
        "last_beast" to "On a planet gone songless, a single beast remained.",
        "star_monk" to "It speaks three words in a lifetime; the third is a warning to strangers.",
        "tyrant_shark" to "The sea's tyrant - it hunts any who enter its waters, great or small.",
        "isle_whale" to "A drifting island, an ecosystem on its back, unmoved by anything.",
        "song_whale" to "A vast body that only sings; it knows nothing of war.",
        "gravity_whale" to "Immeasurably vast, and utterly gentle.",
        "rogue_drifter" to "Nothing is more unsettling than one who only drifts. Before you shoot, blink your lights once.",
        "rust_sovereign" to "What set a crown on the rust titan - a king, of sorts, for a machinery long stopped.",
        "grand_archivist" to "Keeper of a ledger with no delete API. Your deeds, too, are bound in here.",
        "tide_sovereign" to "A crown that rules the tide - like returning waves, it heals and strikes again.",
    )
}
