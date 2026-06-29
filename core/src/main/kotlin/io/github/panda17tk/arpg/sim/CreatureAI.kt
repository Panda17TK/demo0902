package io.github.panda17tk.arpg.sim

/** What a creature is doing right now. Drives whether AISystem fights, runs, hides, rests, begs or guards. */
enum class CreatureState { Hostile, Flee, Beg, Hide, Rest, Protect }

/**
 * Pure decision: given a creature's condition and temperament, what state should it be in?
 * Legacy enemies (bravery 1, no beg/hide, no ward) stay Hostile forever, so existing behaviour is unchanged.
 */
object CreatureAI {
    fun nextState(
        hpFrac: Float, bravery: Float, intelligence: Float,
        canBeg: Boolean, canHideAndRest: Boolean, mercyThreshold: Float,
        protectedThreatened: Boolean, protectiveness: Float,
    ): CreatureState {
        if (protectedThreatened && protectiveness >= PROTECT_MIN) return CreatureState.Protect
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
}
