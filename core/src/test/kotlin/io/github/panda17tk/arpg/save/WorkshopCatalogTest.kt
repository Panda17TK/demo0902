package io.github.panda17tk.arpg.save

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.90 保守員の工房: the catalog's arithmetic — costs, ranks, boons, salvage. */
class WorkshopCatalogTest {
    @Test fun `the catalog is six distinct crafts`() {
        assertEquals(6, WorkshopCatalog.ITEMS.size) // v2.104: +管制核の欠片
        assertEquals(WorkshopCatalog.ITEMS.size, WorkshopCatalog.ITEMS.map { it.id }.distinct().size)
        for (item in WorkshopCatalog.ITEMS) assertTrue(item.maxRank in 1..4 && item.baseCost > 0)
    }

    @Test fun `each rank doubles the price`() {
        val hull = WorkshopCatalog.byId("hull")!!
        assertEquals(60, WorkshopCatalog.cost(hull, 0))
        assertEquals(120, WorkshopCatalog.cost(hull, 1))
        assertEquals(240, WorkshopCatalog.cost(hull, 2))
    }

    @Test fun `boons add up rank by rank and an empty book grants nothing`() {
        val none = WorkshopCatalog.boonsFor(emptyMap())
        assertEquals(0f, none.hull)
        assertEquals(1f, none.reloadMul)
        val full = WorkshopCatalog.boonsFor(mapOf("hull" to 3, "mend" to 2, "hands" to 3, "breath" to 2, "eye" to 2))
        assertEquals(30f, full.hull)
        assertEquals(1f, full.regenPerSec, 1e-4f)
        assertTrue(full.reloadMul in 0.85f..0.87f, "0.95^3 ≈ 0.857 (got ${full.reloadMul})")
        assertEquals(30f, full.stamina)
        assertEquals(0.10f, full.loot, 1e-4f)
    }

    @Test fun `salvage takes a modest tithe and never invents fragments`() {
        assertEquals(0, WorkshopCatalog.salvage(0))
        assertEquals(15, WorkshopCatalog.salvage(100))
        assertTrue(WorkshopCatalog.salvage(7) <= 1)
    }
}
