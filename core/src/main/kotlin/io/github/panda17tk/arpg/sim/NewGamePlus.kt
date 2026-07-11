package io.github.panda17tk.arpg.sim

/**
 * v2.160 周回の印II: a completed sync leaves its mark on the NEXT sky. Each clear starts the
 * surge two waves deeper (pool, quota, pace, hp scale) and levels every surge member once —
 * one more unlocked move (Leveling.attacksForLevel) and sharper smarts. Capped so a veteran
 * account stays playable; the training sim and the proving run always pass 0 clears, so their
 * fairness is untouched. Pure — the sim never reads Preferences; the screen hands clears in.
 */
object NewGamePlus {
    /** Clears beyond this deepen nothing — +6 waves / +3 levels is the ceiling. */
    const val CAP = 3

    /** How many waves deeper a marked account's sky starts. */
    fun depth(clears: Int): Int = clears.coerceIn(0, CAP) * 2

    /** The level every surge member gains — one more unlocked move per clear. */
    fun levelBonus(clears: Int): Int = clears.coerceIn(0, CAP)
}
