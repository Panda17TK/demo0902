package io.github.panda17tk.arpg.config

/**
 * v2.90 保守員の工房 — the permanent boons a run starts with, bought between runs with
 * recovered fragments. Pure data; WorldFactory injects it and the systems read it, so a
 * headless test can hand in any loadout without touching preferences.
 */
data class WorkshopBoons(
    val hull: Float = 0f,        // added to starting max HP
    val regenPerSec: Float = 0f, // added to the 1/s rest mend rate
    val reloadMul: Float = 1f,   // multiplies every reload's duration (smaller = faster)
    val stamina: Float = 0f,     // added to max stamina
    val loot: Float = 0f,        // added to the bonus-material drop chance
) {
    companion object {
        val NONE = WorkshopBoons()
    }
}
