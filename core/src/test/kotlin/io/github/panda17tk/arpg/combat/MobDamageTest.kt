package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Velocity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MobDamageTest {
    @Test fun `damage reduces hp and applies knockback + flash`() {
        val h = Health(50f, 50f); val v = Velocity()
        MobDamage.hurt(h, v, dmg = 20f, nx = 1f, ny = 0f, kb = 100f)
        assertEquals(30f, h.hp, 1e-3f)
        assertEquals(100f, v.vx, 1e-3f)
        assertTrue(h.hitFlash > 0f)
    }
    @Test fun `lethal damage drops hp to zero or below`() {
        val h = Health(10f, 10f); val v = Velocity()
        MobDamage.hurt(h, v, dmg = 25f, nx = 0f, ny = 1f, kb = 0f)
        assertTrue(h.hp <= 0f)
    }
}
