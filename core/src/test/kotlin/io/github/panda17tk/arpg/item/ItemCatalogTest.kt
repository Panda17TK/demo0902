package io.github.panda17tk.arpg.item

import io.github.panda17tk.arpg.combat.Weapons
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ItemCatalogTest {
    @Test fun `item ids are unique`() {
        assertEquals(ItemCatalog.ALL.size, ItemCatalog.ALL.map { it.id }.toSet().size)
    }

    @Test fun `the world holds plenty of items`() {
        assertTrue(ItemCatalog.ALL.size >= 20)
    }

    @Test fun `every slot family has at least one item`() {
        for (kind in ItemKind.values()) {
            assertTrue(ItemCatalog.ALL.any { it.kind == kind }, "no item of kind $kind")
        }
    }

    @Test fun `ranged items name real WeaponDef ids and cover every classic gun`() {
        val weaponIds = Weapons.ALL.map { it.id }.toSet()
        val itemTypes = ItemCatalog.ALL.filter { it.kind == ItemKind.RANGED_WEAPON }.mapNotNull { it.weaponType }
        assertTrue(itemTypes.all { it in weaponIds }, "unknown weaponType in $itemTypes")
        assertEquals(weaponIds, itemTypes.toSet()) // ピストルもマシンガンも「種類」を持つアイテムとして存在する
    }

    @Test fun `an OC thruster exists and unlocks full throttle`() {
        val oc = ItemCatalog.ALL.first { it.thrusterClass == ThrusterClass.OC }
        assertEquals(ItemKind.THRUSTER, oc.kind)
        val l = Loadout(thruster = oc)
        assertTrue(l.hasOverclockThruster)
    }

    @Test fun `byId and byWeaponType resolve`() {
        assertNotNull(ItemCatalog.byId("thruster_oc"))
        assertNull(ItemCatalog.byId("nope"))
        assertEquals("gun_mg", ItemCatalog.byWeaponType("mg")?.id)
    }

    @Test fun `the starter loadout fills the four core slots`() {
        val l = ItemCatalog.starterLoadout()
        assertNotNull(l.thruster); assertNotNull(l.armor); assertNotNull(l.ranged); assertNotNull(l.melee)
        assertEquals("pistol", l.ranged?.weaponType)
    }

    @Test fun `dropFor is total over any roll`() {
        for (roll in listOf(0, 1, 999, -5, Int.MAX_VALUE, Int.MIN_VALUE)) {
            assertNotNull(ItemCatalog.dropFor(roll))
        }
    }
}
