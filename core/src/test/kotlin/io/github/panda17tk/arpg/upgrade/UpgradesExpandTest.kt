package io.github.panda17tk.arpg.upgrade

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.UpgradesConfig
import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.ecs.components.Mods
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.world.MobFactory
import io.github.panda17tk.arpg.ecs.world.Pickups
import io.github.panda17tk.arpg.ecs.world.WorldFactory
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.107 強化カード拡充: sixteen distinct cards, each new dial lands, and the preview reads honest. */
class UpgradesExpandTest {
    private val cfg = UpgradesConfig()

    @Test fun `the pool holds sixteen distinct cards`() {
        assertEquals(16, Upgrades.ALL.size)
        assertEquals(16, Upgrades.ALL.map { it.id }.distinct().size)
    }

    @Test fun `every new card turns its own dial`() {
        val m = Mods(); val h = Health(100f, 100f); val a = Ammo(); val mat = Materials(); val s = Stamina(100f, 100f)
        Upgrades.apply("reload_fast", cfg, m, h, a, mat, s)
        assertEquals(0.8f, m.reloadMul, 1e-4f)
        Upgrades.apply("stamina_up", cfg, m, h, a, mat, s)
        assertEquals(125f, s.max, 1e-3f)
        assertEquals(125f, s.value, 1e-3f, "the gained breath arrives filled")
        Upgrades.apply("blast_up", cfg, m, h, a, mat, s)
        assertEquals(1.25f, m.blastMul, 1e-4f)
        Upgrades.apply("regen_up", cfg, m, h, a, mat, s)
        assertEquals(0.5f, m.regenAdd, 1e-4f)
        Upgrades.apply("dash_eff", cfg, m, h, a, mat, s)
        assertEquals(0.8f, m.dashCostMul, 1e-4f)
        Upgrades.apply("bullet_speed", cfg, m, h, a, mat, s)
        assertEquals(1.2f, m.bulletSpeedMul, 1e-4f)
        Upgrades.apply("armor_up", cfg, m, h, a, mat, s)
        assertEquals(0.9f, m.armorMul, 1e-4f)
        Upgrades.apply("magnet_up", cfg, m, h, a, mat, s)
        assertEquals(24f, m.pickupRange, 1e-3f)
        // stacking is multiplicative (or additive) — a second card keeps moving the dial
        Upgrades.apply("armor_up", cfg, m, h, a, mat, s)
        assertEquals(0.81f, m.armorMul, 1e-4f)
    }

    @Test fun `every card describes itself and previews YOUR numbers`() {
        val m = Mods(gunMul = 1.25f); val h = Health(100f, 100f); val s = Stamina(100f, 100f)
        for (u in Upgrades.ALL) {
            assertTrue(Upgrades.desc(u, cfg).isNotEmpty(), "${u.id} has a description")
            val p = Upgrades.preview(u, cfg, m, h, s)
            assertTrue(p.contains("→"), "${u.id} previews before → after (got $p)")
        }
        assertTrue(Upgrades.preview(Upgrades.byId("gun_dmg")!!, cfg, m, h, s).startsWith("×1.25"), "the CURRENT value leads")
    }

    @Test fun `回収の手 widens the pickup reach in the running sim`() {
        val input = InputState()
        val gw = WorldFactory.create(input, seed = 1L)
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        with(gw.world) { gw.player[Mods].pickupRange = 200f }
        Pickups.spawn(gw.world, "dust", 5, px + 150f, py)
        val before = with(gw.world) { gw.player[Materials].dust }
        gw.world.update(1f / 60f)
        val after = with(gw.world) { gw.player[Materials].dust }
        assertEquals(before + 5, after, "150px away is within the widened hand")
    }

    @Test fun `装甲圧延 thins what the maintenance protocols can take`() {
        // A zero-armor keeper takes NO contact-attack damage; the dial reaches the damage path.
        val input = InputState()
        val gw = WorldFactory.create(input, seed = 1L)
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        with(gw.world) { gw.player[Mods].armorMul = 0f }
        val biter = GameConfig().enemies.getValue("zombie").copy(hp = 5000f)
        MobFactory.spawn(gw.world, biter, px + 10f, py)
        val hp0 = with(gw.world) { gw.player[Health].hp }
        repeat(120) { gw.world.update(1f / 60f) } // 2s beside a biting zombie
        val hp1 = with(gw.world) { gw.player[Health].hp }
        assertEquals(hp0, hp1, 0.5f, "armorMul=0 → the bites cost nothing (magma/etc aside)")
    }
}
