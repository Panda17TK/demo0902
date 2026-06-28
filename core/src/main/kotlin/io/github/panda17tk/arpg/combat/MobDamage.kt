package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Velocity

/** Single choke-point for damaging a mob (legacy combat-core.hurtMob). Dodge/guard: Phase 5b. */
object MobDamage {
    fun hurt(health: Health, vel: Velocity, dmg: Float, nx: Float, ny: Float, kb: Float) {
        health.hp -= dmg
        if (kb != 0f) { vel.vx += nx * kb; vel.vy += ny * kb }
        health.hitFlash = 0.12f
    }
}
