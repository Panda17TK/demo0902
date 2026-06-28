package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.config.PlayerConfig

/** Resolved melee strength for a stamina ratio (legacy melee.js tiers). */
data class MeleeOutcome(val dmg: Float, val slashDmg: Float, val isFist: Boolean)

object MeleeResolve {
    fun resolve(staRatio: Float, cfg: PlayerConfig): MeleeOutcome = when {
        staRatio <= cfg.meleeStaSwordMin -> MeleeOutcome(cfg.fistDmg, 0f, true)
        staRatio < cfg.meleeStaWeakBelow -> MeleeOutcome(
            cfg.meleeDmg * cfg.meleeWeakMul, cfg.meleeSlashDmg * cfg.meleeWeakMul, false,
        )
        else -> MeleeOutcome(cfg.meleeDmg, cfg.meleeSlashDmg, false)
    }
}
