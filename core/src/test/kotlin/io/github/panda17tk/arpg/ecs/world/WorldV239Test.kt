package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.Bullet
import io.github.panda17tk.arpg.ecs.components.Cooldowns
import io.github.panda17tk.arpg.ecs.components.Gear
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.item.EquipSlot
import io.github.panda17tk.arpg.item.ItemCatalog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.39: more planets, richer space roster, beam charge, melee wave. */
class WorldV239Test {
    @Test fun `space carries 6 to 8 planets with varied sizes`() {
        val gw = WorldFactory.create(InputState(), seed = 5L)
        assertTrue(gw.planets.size in 6..8, "got ${gw.planets.size} planets")
        val radii = gw.planets.map { it.radius }
        assertTrue(radii.max() - radii.min() > 20f, "planet sizes should vary: $radii")
    }

    @Test fun `the space roster carries the new varied enemies`() {
        val enemies = GameConfig().enemies
        for (key in listOf("lancer", "mortar_bug", "shield_bearer", "mite", "marksman", "sapper")) {
            val def = enemies[key]
            assertTrue(def != null && def.tier == "normal" && def.biome == null, "$key should be a space normal")
        }
        // Variety: the tiny mite and the walking wall differ hugely in size and nerve.
        assertTrue(enemies["mite"]!!.w < enemies["shield_bearer"]!!.w / 2f)
        assertTrue(enemies["mite"]!!.intelligence < enemies["marksman"]!!.intelligence)
        assertTrue(enemies["mortar_bug"]!!.bravery < enemies["lancer"]!!.bravery)
    }

    @Test fun `the v2_41 additions are in the roster with their new attack types`() {
        val enemies = GameConfig().enemies
        assertEquals("spiral", enemies["prism"]!!.attacks.single().type)
        assertEquals("spray", enemies["sprayer"]!!.attacks.single().type)
        assertTrue(enemies["duelist"]!!.attacks.any { it.type == "twin_shot" })
        assertTrue(enemies["warden"]!!.attacks.any { it.type == "shockwave" })
        assertEquals("midboss", enemies["artillery"]!!.tier)
        assertTrue(enemies["artillery"]!!.attacks.any { it.type == "shockwave" })
    }

    @Test fun `aiming a beam builds charge and firing spends it`() {
        val input = InputState()
        val gw = WorldFactory.create(input, seed = 3L)
        with(gw.world) {
            val ars = gw.player[Arsenal]
            ars.curW = ars.weapons.indexOfFirst { it.def.id == "beam" }
        }
        input.aiming = true
        repeat(30) { gw.world.update(1f / 60f) } // half a second of aiming
        val charged = with(gw.world) { gw.player[Cooldowns].beamCharge }
        assertTrue(charged > 0.2f, "charge should build while aiming, got $charged")
        input.fireRelease = true
        gw.world.update(1f / 60f)
        with(gw.world) { assertEquals(0f, gw.player[Cooldowns].beamCharge, 1e-4f) } // spent on the shot
        input.fireRelease = false
    }

    @Test fun `switching away from the beam drops the charge`() {
        val input = InputState()
        val gw = WorldFactory.create(input, seed = 3L)
        with(gw.world) {
            val ars = gw.player[Arsenal]
            ars.curW = ars.weapons.indexOfFirst { it.def.id == "beam" }
        }
        input.aiming = true
        repeat(10) { gw.world.update(1f / 60f) }
        with(gw.world) { gw.player[Arsenal].curW = 0 } // back to the pistol
        gw.world.update(1f / 60f)
        with(gw.world) { assertEquals(0f, gw.player[Cooldowns].beamCharge, 1e-4f) }
    }

    @Test fun `a resonant blade throws a three-shard slash wave`() {
        val input = InputState()
        val gw = WorldFactory.create(input, seed = 3L)
        with(gw.world) {
            val gear = gw.player[Gear]
            val prev = gear.loadout.set(EquipSlot.MELEE, ItemCatalog.byId("melee_resonant")!!)
            if (prev != null) gear.backpack.add(prev)
        }
        input.melee = true
        gw.world.update(1f / 60f)
        assertEquals(3, gw.world.family { all(Bullet) }.numEntities)
    }
}
