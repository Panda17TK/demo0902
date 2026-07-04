package io.github.panda17tk.arpg.item

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.47 武器合成: honed defs derive from their base; ids round-trip through the catalog. */
class GearCraftTest {
    private fun gun() = ItemCatalog.ALL.first { it.kind == ItemKind.RANGED_WEAPON }
    private fun blade() = ItemCatalog.ALL.first { it.kind == ItemKind.MELEE_WEAPON }

    @Test fun `id parsing splits base and level`() {
        assertEquals("gun_mg", GearCraft.baseId("gun_mg+2"))
        assertEquals(2, GearCraft.level("gun_mg+2"))
        assertEquals("gun_mg", GearCraft.baseId("gun_mg"))
        assertEquals(0, GearCraft.level("gun_mg"))
    }

    @Test fun `honing sharpens damage and handling`() {
        val base = gun()
        val up = GearCraft.honed(base)
        assertEquals(base.id + "+1", up.id)
        assertTrue(up.name.endsWith("+1"))
        assertTrue(up.gunMul > base.gunMul)
        assertTrue(up.fireRateMul < base.fireRateMul)
        assertTrue(up.reloadMul < base.reloadMul)
        val up2 = GearCraft.honed(up)
        assertEquals(base.id + "+2", up2.id)
        assertTrue(up2.gunMul > up.gunMul)
    }

    @Test fun `melee hones its damage too`() {
        val up = GearCraft.honed(blade())
        assertTrue(up.meleeDmgMul > blade().meleeDmgMul)
    }

    @Test fun `the hone cap stops crafting at +3`() {
        var d = gun()
        repeat(3) {
            assertTrue(GearCraft.craftable(d), "${d.id} should still be craftable")
            d = GearCraft.honed(d)
        }
        assertEquals(3, GearCraft.level(d.id))
        assertFalse(GearCraft.craftable(d), "+3 is the ceiling")
    }

    @Test fun `only weapons hone`() {
        val armor = ItemCatalog.ALL.first { it.kind == ItemKind.ARMOR }
        assertFalse(GearCraft.craftable(armor))
    }

    @Test fun `honed ids resolve through the catalog (saves keep working)`() {
        val id = gun().id + "+2"
        val resolved = ItemCatalog.byId(id)
        assertNotNull(resolved)
        assertEquals(id, resolved!!.id)
        assertTrue(resolved.gunMul > gun().gunMul)
        assertNull(ItemCatalog.byId("no_such_gun+1"))
    }
}
