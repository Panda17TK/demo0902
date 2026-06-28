package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.MobAction
import io.github.panda17tk.arpg.ecs.components.Velocity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MobDamageTest {
    @Test fun `damage reduces hp and applies knockback + flash`() {
        val h = Health(50f, 50f); val v = Velocity(); val a = MobAction()
        MobDamage.hurt(h, v, a, dodge = null, dmg = 20f, nx = 1f, ny = 0f, kb = 100f, dodgeRoll = 0.99f)
        assertEquals(30f, h.hp, 1e-3f)
        assertEquals(100f, v.vx, 1e-3f)
        assertTrue(h.hitFlash > 0f)
    }
    @Test fun `lethal damage drops hp to zero or below`() {
        val h = Health(10f, 10f); val v = Velocity(); val a = MobAction()
        MobDamage.hurt(h, v, a, dodge = null, dmg = 25f, nx = 0f, ny = 1f, kb = 0f, dodgeRoll = 0.99f)
        assertTrue(h.hp <= 0f)
    }
}
