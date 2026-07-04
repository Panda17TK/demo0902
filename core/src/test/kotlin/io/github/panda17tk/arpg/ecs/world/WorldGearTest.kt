package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.Gear
import io.github.panda17tk.arpg.ecs.components.Pickup
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.item.EquipSlot
import io.github.panda17tk.arpg.item.ItemCatalog
import io.github.panda17tk.arpg.math.Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldGearTest {
    @Test fun `the player spawns wearing the starter loadout with a stocked backpack`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        val gear = with(gw.world) { gw.player[Gear] }
        assertEquals("gun_pistol", gear.loadout.ranged?.id)
        assertEquals("melee_knife", gear.loadout.melee?.id)
        assertTrue(gear.backpack.any { it.id == "thruster_oc" }) // full throttle is there to try
    }

    @Test fun `cycling the RANGED slot swaps in a backpack gun and points Arsenal at its weapon type`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        val gear = with(gw.world) { gw.player[Gear] }
        val name = GearOps.cycleSlot(gw.world, gw.player, EquipSlot.RANGED)
        assertNotNull(name)
        val equipped = gear.loadout.ranged!!
        val arsenal = with(gw.world) { gw.player[Arsenal] }
        assertEquals(equipped.weaponType, arsenal.current.def.id) // 武器切替 = 装備の入れ替え
        assertTrue(gear.backpack.any { it.id == "gun_pistol" }) // the pistol went back into the backpack
    }

    @Test fun `cycling a slot with no compatible item leaves it untouched`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        val gear = with(gw.world) { gw.player[Gear] }
        gear.backpack.removeAll { it.id.startsWith("gun_") || it.id.startsWith("melee_") }
        val before = gear.loadout.melee?.id
        assertNull(GearOps.cycleSlot(gw.world, gw.player, EquipSlot.MELEE))
        assertEquals(before, gear.loadout.melee?.id)
    }

    @Test fun `PlayerCarry hauls the loadout and backpack across a world rebuild`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        val gear = with(gw.world) { gw.player[Gear] }
        GearOps.cycleSlot(gw.world, gw.player, EquipSlot.THRUSTER) // equip something non-default
        val equippedThruster = gear.loadout.thruster?.id
        val carry = PlayerCarry.of(gw.world, gw.player, wave = 4)
        val gw2 = WorldFactory.create(InputState(), seed = 4L, carry = carry)
        val gear2 = with(gw2.world) { gw2.player[Gear] }
        assertEquals(equippedThruster, gear2.loadout.thruster?.id)
        assertEquals(gear.backpack.map { it.id }, gear2.backpack.map { it.id })
    }

    @Test fun `walking over an item pickup stores it in the backpack`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        val (px, py) = with(gw.world) { val t = gw.player[Transform]; t.x to t.y }
        Pickups.spawn(gw.world, "item:acc_boots", 1, px, py)
        val before = with(gw.world) { gw.player[Gear] }.backpack.count { it.id == "acc_boots" }
        gw.world.update(1f / 60f)
        val after = with(gw.world) { gw.player[Gear] }.backpack.count { it.id == "acc_boots" }
        assertEquals(before + 1, after)
    }

    @Test fun `a boss kill always drops an equipment item pickup`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        fun itemKinds(): List<String> {
            val kinds = mutableListOf<String>()
            with(gw.world) {
                gw.world.family { all(Pickup, Transform) }.forEach { kinds.add(it[Pickup].kind) }
            }
            return kinds.filter { it.startsWith("item:") }
        }
        val before = itemKinds() // v2.46: wrecks pre-seed weapon caches — measure the kill's delta
        Pickups.dropOnKill(gw.world, Rng(1L), 500f, 500f, boss = true)
        val after = itemKinds()
        assertEquals(before.size + 1, after.size)
        after.forEach { assertNotNull(ItemCatalog.byId(it.removePrefix("item:"))) } // every id resolves
    }
}
