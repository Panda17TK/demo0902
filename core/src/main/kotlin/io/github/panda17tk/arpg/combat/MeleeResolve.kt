package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.sim.Tuning

/** Resolved melee strength for a stamina ratio (legacy melee.js tiers). */
data class MeleeOutcome(val dmg: Float, val slashDmg: Float, val isFist: Boolean)

object MeleeResolve {
    fun resolve(staRatio: Float): MeleeOutcome = when {
        staRatio <= Tuning.MELEE_STA_SWORD_MIN -> MeleeOutcome(Tuning.FIST_DMG, 0f, true)
        staRatio < Tuning.MELEE_STA_WEAK_BELOW -> MeleeOutcome(
            Tuning.MELEE_DMG * Tuning.MELEE_WEAK_MUL, Tuning.MELEE_SLASH_DMG * Tuning.MELEE_WEAK_MUL, false,
        )
        else -> MeleeOutcome(Tuning.MELEE_DMG, Tuning.MELEE_SLASH_DMG, false)
    }
}
