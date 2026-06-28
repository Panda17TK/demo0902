package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.config.DodgeSpec
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.MobAction
import io.github.panda17tk.arpg.ecs.components.Velocity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MobDamageDodgeTest {
    @Test fun `no dodge spec applies damage and lands`() {
        val h = Health(50f, 50f); val v = Velocity(); val a = MobAction()
        val landed = MobDamage.hurt(h, v, a, dodge = null, dmg = 20f, nx = 1f, ny = 0f, kb = 0f, dodgeRoll = 0.99f)
        assertTrue(landed); assertEquals(30f, h.hp, 1e-3f)
    }
    @Test fun `dodge roll under chance negates the hit and starts invuln`() {
        val h = Health(50f, 50f); val v = Velocity(); val a = MobAction()
        val landed = MobDamage.hurt(h, v, a, dodge = DodgeSpec(0.18f, 0.15f, 2f), dmg = 20f, nx = 1f, ny = 0f, kb = 0f, dodgeRoll = 0.10f)
        assertFalse(landed); assertEquals(50f, h.hp, 1e-3f); assertTrue(a.dodgeT > 0f); assertTrue(a.dodgeCd > 0f)
    }
    @Test fun `dodge on cooldown does not negate`() {
        val h = Health(50f, 50f); val v = Velocity(); val a = MobAction(dodgeCd = 1f)
        val landed = MobDamage.hurt(h, v, a, dodge = DodgeSpec(0.18f, 0.15f, 2f), dmg = 20f, nx = 1f, ny = 0f, kb = 0f, dodgeRoll = 0.01f)
        assertTrue(landed); assertEquals(30f, h.hp, 1e-3f)
    }
}
