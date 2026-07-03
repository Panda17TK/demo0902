package io.github.panda17tk.arpg.item

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoadoutTest {
    @Test fun `set rejects an incompatible kind`() {
        val l = Loadout()
        assertThrows(IllegalArgumentException::class.java) {
            l.set(EquipSlot.ARMOR, ItemCatalog.byId("gun_pistol")!!)
        }
    }

    @Test fun `set returns the previous piece (swap semantics)`() {
        val l = ItemCatalog.starterLoadout()
        val prev = l.set(EquipSlot.RANGED, ItemCatalog.byId("gun_mg")!!)
        assertEquals("gun_pistol", prev?.id)
        assertEquals("gun_mg", l.ranged?.id)
    }

    @Test fun `accessories occupy three independent slots`() {
        val l = Loadout()
        val boots = ItemCatalog.byId("acc_boots")!!
        val charm = ItemCatalog.byId("acc_charm")!!
        l.set(EquipSlot.ACC1, boots); l.set(EquipSlot.ACC2, charm)
        assertEquals(boots, l.get(EquipSlot.ACC1))
        assertEquals(charm, l.get(EquipSlot.ACC2))
        assertNull(l.get(EquipSlot.ACC3))
    }

    @Test fun `damage taken multiplies across armor and accessories`() {
        val l = Loadout(armor = ItemCatalog.byId("armor_light")) // 0.85
        l.set(EquipSlot.ACC1, ItemCatalog.byId("acc_plating")!!) // 0.9
        assertEquals(0.85f * 0.9f, l.damageTakenMul, 1e-4f)
    }

    @Test fun `thruster multipliers come from the thruster alone`() {
        val l = Loadout(thruster = ItemCatalog.byId("thruster_light"))
        assertEquals(1.3f, l.thrustAccelMul, 1e-4f)
        assertEquals(0.9f, l.thrustCruiseMul, 1e-4f)
        assertFalse(l.hasOverclockThruster)
    }

    @Test fun `an empty loadout is all-neutral`() {
        val l = Loadout()
        assertEquals(1f, l.damageTakenMul, 1e-6f)
        assertEquals(1f, l.moveMul, 1e-6f)
        assertEquals(1f, l.gunMul, 1e-6f)
        assertEquals(1f, l.meleeDmgMul, 1e-6f)
        assertEquals(1f, l.meleeReachMul, 1e-6f)
    }

    @Test fun `full throttle trades 3x thrust and 2x cruise for 2x stamina`() {
        assertEquals(3f, FullThrottle.ACCEL_MUL, 1e-6f)
        assertEquals(2f, FullThrottle.CRUISE_MUL, 1e-6f)
        assertEquals(2f, FullThrottle.DRAIN_MUL, 1e-6f)
    }

    @Test fun `melee stats come from the melee arm`() {
        val l = Loadout(melee = ItemCatalog.byId("melee_lance"))
        assertEquals(1.4f, l.meleeReachMul, 1e-4f)
        assertTrue(l.meleeDmgMul > 1f)
    }

    @Test fun `traits come from any equipped piece - armor or accessory`() {
        val armor = Loadout(armor = ItemCatalog.byId("armor_thermal"))
        assertTrue(armor.has(ItemTrait.HEAT_PROOF))
        assertFalse(armor.has(ItemTrait.COLD_PROOF))
        val acc = Loadout()
        acc.set(EquipSlot.ACC2, ItemCatalog.byId("acc_gripsole")!!)
        assertTrue(acc.has(ItemTrait.COLD_PROOF))
        assertFalse(Loadout().has(ItemTrait.MAGNET))
    }

    @Test fun `hp regen stacks additively across equipped pieces`() {
        val l = Loadout()
        assertEquals(0f, l.hpRegen, 1e-6f)
        l.set(EquipSlot.ACC1, ItemCatalog.byId("acc_repair")!!)
        l.set(EquipSlot.ACC2, ItemCatalog.byId("acc_repair")!!)
        assertEquals(3f, l.hpRegen, 1e-4f)
    }
}
