package io.github.panda17tk.arpg.item

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MarketTest {
    @Test fun `a hostile world closes its stalls`() {
        assertTrue(Market.isOpen(0f))
        assertTrue(Market.isOpen(0.49f))
        assertFalse(Market.isOpen(0.5f))
        assertFalse(Market.isOpen(1f))
    }

    @Test fun `the stock is deterministic per planet and never sells stories`() {
        val a = Market.stockFor(42L)
        val b = Market.stockFor(42L)
        assertEquals(a, b)
        assertEquals(Market.SLOTS, a.size)
        assertTrue(a.none { it.kind == ItemKind.LORE })
        assertTrue(Market.stockFor(43L) != a || Market.stockFor(44L) != a) // different worlds, different stalls
    }

    @Test fun `gratitude buys a discount`() {
        val item = ItemCatalog.byId("armor_combat")!!
        val full = Market.priceFor(item, mercy = 0f)
        val loved = Market.priceFor(item, mercy = 1f)
        assertTrue(loved < full, "mercy should discount: $full -> $loved")
        assertEquals((full * 0.6f).toInt(), loved)
    }

    @Test fun `special gear commands a premium`() {
        val plain = Market.priceFor(ItemCatalog.byId("armor_combat")!!, 0f)
        val traited = Market.priceFor(ItemCatalog.byId("armor_thermal")!!, 0f)
        assertTrue(traited > plain)
        assertTrue(Market.priceFor(ItemCatalog.byId("thruster_oc")!!, 0f) > Market.priceFor(ItemCatalog.byId("thruster_std")!!, 0f))
    }
}
