package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.config.FamilyRole

/** Pure tribe-society helpers: who is a protectable ward, and how a nearby king steels morale. */
object Family {
    /** Wards are the vulnerable/important members guardians rally to defend. */
    fun isWard(role: FamilyRole): Boolean =
        role == FamilyRole.CHILD || role == FamilyRole.ELDER || role == FamilyRole.KING

    /** A nearby king raises a creature's effective bravery so allies hold the line instead of fleeing. */
    fun effectiveBravery(bravery: Float, kingNear: Boolean): Float =
        if (kingNear) minOf(1f, bravery + KING_BRAVERY) else bravery

    const val KING_BRAVERY = 0.5f
}
