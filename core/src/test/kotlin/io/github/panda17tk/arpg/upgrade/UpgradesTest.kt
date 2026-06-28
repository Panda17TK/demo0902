package io.github.panda17tk.arpg.upgrade

import io.github.panda17tk.arpg.config.UpgradesConfig
import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.ecs.components.Mods
import io.github.panda17tk.arpg.math.Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UpgradesTest {
    private val cfg = UpgradesConfig()

    @Test fun `pick returns n distinct upgrades`() {
        val picks = Upgrades.pick(3, Rng(1L))
        assertEquals(3, picks.size)
        assertEquals(3, picks.map { it.id }.toSet().size, "choices must not repeat")
    }

    @Test fun `pick is deterministic for a given seed`() {
        assertEquals(Upgrades.pick(3, Rng(42L)).map { it.id }, Upgrades.pick(3, Rng(42L)).map { it.id })
    }

    @Test fun `pick caps at pool size`() {
        assertEquals(Upgrades.ALL.size, Upgrades.pick(99, Rng(1L)).size)
    }

    @Test fun `gun_dmg multiplies gunMul`() {
        val m = Mods(); Upgrades.apply("gun_dmg", cfg, m, Health(100f, 100f), Ammo(), Materials())
        assertEquals(1.25f, m.gunMul, 1e-4f)
    }

    @Test fun `fire_rate multiplies fireMul down`() {
        val m = Mods(); Upgrades.apply("fire_rate", cfg, m, Health(100f, 100f), Ammo(), Materials())
        assertEquals(0.85f, m.fireMul, 1e-4f)
    }

    @Test fun `melee multiplies meleeMul`() {
        val m = Mods(); Upgrades.apply("melee", cfg, m, Health(100f, 100f), Ammo(), Materials())
        assertEquals(1.35f, m.meleeMul, 1e-4f)
    }

    @Test fun `max_hp raises hpMax and full-heals`() {
        val h = Health(30f, 100f); Upgrades.apply("max_hp", cfg, Mods(), h, Ammo(), Materials())
        assertEquals(125f, h.hpMax, 1e-4f); assertEquals(125f, h.hp, 1e-4f)
    }

    @Test fun `speed multiplies moveMul`() {
        val m = Mods(); Upgrades.apply("speed", cfg, m, Health(100f, 100f), Ammo(), Materials())
        assertEquals(1.12f, m.moveMul, 1e-4f)
    }

    @Test fun `ammo refills pools and raises ammoMul`() {
        val m = Mods(); val a = Ammo(ammo9 = 0, ammo12 = 0, ammoBeam = 0, ammoNade = 0)
        Upgrades.apply("ammo", cfg, m, Health(100f, 100f), a, Materials())
        assertEquals(1.5f, m.ammoMul, 1e-4f)
        assertEquals(40, a.ammo9); assertEquals(8, a.ammo12); assertEquals(2, a.ammoBeam); assertEquals(1, a.ammoNade)
    }

    @Test fun `lifesteal adds healOnKill`() {
        val m = Mods(); Upgrades.apply("lifesteal", cfg, m, Health(100f, 100f), Ammo(), Materials())
        assertEquals(2f, m.healOnKill, 1e-4f)
    }

    @Test fun `engineer adds blocks and wall hp`() {
        val m = Mods(); val mat = Materials(); val base = mat.blocks
        Upgrades.apply("engineer", cfg, m, Health(100f, 100f), Ammo(), mat)
        assertEquals(base + 4, mat.blocks); assertEquals(110f, m.wallHp, 1e-4f) // 70 + 40
    }

    @Test fun `stacking gun_dmg multiplies repeatedly`() {
        val m = Mods()
        repeat(2) { Upgrades.apply("gun_dmg", cfg, m, Health(100f, 100f), Ammo(), Materials()) }
        assertEquals(1.25f * 1.25f, m.gunMul, 1e-4f)
    }
}
