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

    @Test fun `drops are weighted consumable, equipment, then lore`() {
        assertEquals(ItemKind.CONSUMABLE, ItemCatalog.dropFor(0).kind)
        assertEquals(ItemKind.CONSUMABLE, ItemCatalog.dropFor(549).kind)
        assertTrue(ItemCatalog.dropFor(550).kind !in setOf(ItemKind.CONSUMABLE, ItemKind.LORE))
        assertTrue(ItemCatalog.dropFor(849).kind !in setOf(ItemKind.CONSUMABLE, ItemKind.LORE))
        assertEquals(ItemKind.LORE, ItemCatalog.dropFor(850).kind)
        assertEquals(ItemKind.LORE, ItemCatalog.dropFor(999).kind)
    }

    @Test fun `every consumable does something and every lore item has text`() {
        for (c in ItemCatalog.ALL.filter { it.kind == ItemKind.CONSUMABLE }) {
            assertTrue(c.consume != ConsumeKind.NONE && c.power > 0f, "${c.id} has no effect")
        }
        for (l in ItemCatalog.ALL.filter { it.kind == ItemKind.LORE }) {
            assertTrue(l.lore.isNotBlank(), "${l.id} has no lore text")
        }
        assertTrue(ItemCatalog.ALL.count { it.kind == ItemKind.CONSUMABLE } >= 10)
        assertTrue(ItemCatalog.ALL.count { it.kind == ItemKind.LORE } >= 10)
    }

    @Test fun `non-consumables never carry a consume effect`() {
        for (i in ItemCatalog.ALL.filter { it.kind != ItemKind.CONSUMABLE }) {
            assertEquals(ConsumeKind.NONE, i.consume, i.id)
        }
    }

    @Test fun `grouped folds duplicates in first-encounter order`() {
        val boots = ItemCatalog.byId("acc_boots")!!
        val spray = ItemCatalog.byId("med_spray")!!
        val groups = ItemCatalog.grouped(listOf(boots, spray, boots))
        assertEquals(listOf("acc_boots" to 2, "med_spray" to 1), groups.map { it.first.id to it.second })
    }

    @Test fun `the starter backpack teaches the world - it carries the mechanic's letter`() {
        assertTrue(ItemCatalog.starterBackpack().any { it.kind == ItemKind.LORE })
        assertTrue(ItemCatalog.starterBackpack().any { it.kind == ItemKind.CONSUMABLE })
    }
}
