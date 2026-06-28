package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.config.DodgeSpec
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.MobAction
import io.github.panda17tk.arpg.ecs.components.Velocity

/** Single choke-point for damaging a mob (legacy combat-core.hurtMob), including the dodge passive. */
object MobDamage {
    /** Returns true if the hit landed (false if dodged / mid-dodge invuln). [dodgeRoll] is rng.nextFloat(). */
    fun hurt(
        health: Health,
        vel: Velocity,
        action: MobAction,
        dodge: DodgeSpec?,
        dmg: Float,
        nx: Float,
        ny: Float,
        kb: Float,
        dodgeRoll: Float,
    ): Boolean {
        if (action.dodgeT > 0f) return false
        if (dodge != null && action.dodgeCd <= 0f && dodgeRoll < dodge.chance) {
            action.dodgeT = dodge.duration
            action.dodgeCd = dodge.cd
            return false
        }
        val realDmg = if (action.guardT > 0f) dmg * action.guardMul else dmg
        health.hp -= realDmg
        if (kb != 0f) { vel.vx += nx * kb; vel.vy += ny * kb }
        health.hitFlash = 0.12f
        return true
    }
}
