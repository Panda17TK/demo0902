package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.MobAction
import io.github.panda17tk.arpg.ecs.components.Velocity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MobDamageGuardTest {
    @Test fun `guard reduces incoming damage by guardMul`() {
        val h = Health(100f, 100f); val a = MobAction(guardT = 2f, guardMul = 0.4f)
        val landed = MobDamage.hurt(h, Velocity(), a, dodge = null, dmg = 20f, nx = 1f, ny = 0f, kb = 0f, dodgeRoll = 0.99f)
        assertTrue(landed); assertEquals(92f, h.hp, 1e-3f) // 100 - 20*0.4
    }

    @Test fun `expired guard takes full damage`() {
        val h = Health(100f, 100f); val a = MobAction(guardT = 0f, guardMul = 0.4f)
        MobDamage.hurt(h, Velocity(), a, dodge = null, dmg = 20f, nx = 1f, ny = 0f, kb = 0f, dodgeRoll = 0.99f)
        assertEquals(80f, h.hp, 1e-3f)
    }
}
