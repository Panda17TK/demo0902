package io.github.panda17tk.arpg.ecs.components

/** Mutable run flag injected into the world (set when the player dies). UI overlay is Phase 7.
 *  v2.92: feat counters ride here too — sim systems count, the screen turns counts into unlocks. */
class GameOver(
    var isOver: Boolean = false,
    var kills: Int = 0,
    var rogueKills: Int = 0, // v2.92: fallen rogue drifters
    var rageKills: Int = 0,  // v2.92: heavies felled while raging (phase 2)
    var grandKills: Int = 0, // v2.92: boss-grade ritual deaths witnessed
    /** v2.113 図鑑: this world's kills by enemy kind — the screen folds them into Bestiary. */
    val killsByKind: MutableMap<String, Int> = mutableMapOf(),
)
