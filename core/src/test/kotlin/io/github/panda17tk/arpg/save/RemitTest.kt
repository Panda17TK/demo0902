package io.github.panda17tk.arpg.save

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.154 星屑の送金: surviving a jump banks more than dying ever did — the economy points forward. */
class RemitTest {
    @Test fun `the jump wires thirty percent and beats the death salvage`() {
        assertEquals(30, WorkshopCatalog.remit(100))
        assertEquals(0, WorkshopCatalog.remit(0))
        assertEquals(0, WorkshopCatalog.remit(3)) // 0.9 truncates — tiny purses keep their crumbs
        assertTrue(WorkshopCatalog.remit(1000) > WorkshopCatalog.salvage(1000),
            "pressing on must bank more than falling")
    }
}
