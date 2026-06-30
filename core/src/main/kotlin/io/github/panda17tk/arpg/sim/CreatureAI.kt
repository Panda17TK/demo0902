package io.github.panda17tk.arpg.sim

/**
 * What a creature is doing right now. Drives whether AISystem fights, warns, runs, hides, rests,
 * begs, guards, rallies, surrenders or simply minds its own business.
 */
enum class CreatureState { Hostile, Warn, Flee, Beg, Hide, Rest, Protect, Rally, Surrender, Ignore }

/**
 * Pure decision: given a creature's condition and temperament, what state should it be in?
 * Legacy enemies (bravery 1, no beg/hide, no ward, not warning) stay Hostile forever, so existing
 * behaviour is unchanged. The Living-Planets signals all default to "off", so existing callers and
 * tests keep their results; only creatures wired with the new traits react.
 */
object CreatureAI {
    fun nextState(
        hpFrac: Float, bravery: Float, intelligence: Float,
        canBeg: Boolean, canHideAndRest: Boolean, mercyThreshold: Float,
        protectedThreatened: Boolean, protectiveness: Float,
        wardHurt: Boolean = false,   // a nearby ward/king is wounded → fierce protectors fly into a fury
        playerNear: Boolean = false, // the player has crossed into warning range
        canWarn: Boolean = false,    // territorial/authoritative creature that has not been provoked yet
    ): CreatureState {
        // A wounded ward (or endangered king) rallies its fierce defenders — overrides flee/beg.
        if (wardHurt && protectiveness >= RALLY_MIN) return CreatureState.Rally
        // A guardian shields a ward the player is crowding.
        if (protectedThreatened && protectiveness >= PROTECT_MIN) return CreatureState.Protect
        // Territorial, still-unprovoked creatures warn before they draw blood.
        if (canWarn && playerNear) return CreatureState.Warn
        if (hpFrac <= LOW_HP) {
            if (canHideAndRest && intelligence >= HIGH_INTEL) return CreatureState.Hide
            if (bravery < BRAVE) return if (canBeg && hpFrac <= mercyThreshold) CreatureState.Beg else CreatureState.Flee
        }
        return CreatureState.Hostile
    }

    private const val LOW_HP = 0.35f     // below this HP fraction a creature may break
    private const val HIGH_INTEL = 0.6f  // smart enough to hide-and-rest instead of panicking
    private const val BRAVE = 0.4f       // at/above this bravery it never flees
    private const val PROTECT_MIN = 0.5f // protectiveness needed to guard a threatened ward
    private const val RALLY_MIN = 0.5f   // protectiveness needed to rally for a wounded ward
}
