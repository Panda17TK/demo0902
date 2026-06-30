package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlanetMemoryBookTest {
    @Test fun `an unvisited planet recalls a blank state`() {
        val book = PlanetMemoryBook()
        assertFalse(book.knows(1L))
        val s = book.recall(1L)
        assertFalse(s.childHarmed)
        assertEquals(0f, s.hostility)
    }

    @Test fun `remember then recall carries the deeds and gauges`() {
        val book = PlanetMemoryBook()
        val visit = book.recall(5L).also { it.onChildKilled() }
        book.remember(5L, visit)
        assertTrue(book.knows(5L))
        val again = book.recall(5L)
        assertTrue(again.childKilled)
        assertTrue(again.childHarmed)
        assertTrue(again.hostility > 0f)
    }

    @Test fun `recall returns a copy — mutating it does not change the book`() {
        val book = PlanetMemoryBook()
        book.remember(2L, PlanetSocietyState().also { it.onApexKilled() })
        val recalled = book.recall(2L)
        recalled.onChildKilled() // mutate the copy
        assertFalse(book.recall(2L).childKilled, "the book must not be aliased by recall()")
    }

    @Test fun `mergeFrom ORs booleans and keeps the max gauge`() {
        val persistent = PlanetSocietyState(hostility = 0.5f).also { it.relicClaimed = true }
        val visit = PlanetSocietyState(hostility = 0.2f).also { it.apexKilled = true }
        persistent.mergeFrom(visit)
        assertTrue(persistent.relicClaimed) // kept
        assertTrue(persistent.apexKilled)   // OR'd in
        assertEquals(0.5f, persistent.hostility) // max(0.5, 0.2)
    }

    @Test fun `mergeFrom keeps the max counter (no double-count from a seeded visit)`() {
        val persistent = PlanetSocietyState(surrenderedSpared = 2)
        val visit = PlanetSocietyState(surrenderedSpared = 3) // seeded from 2, spared one more
        persistent.mergeFrom(visit)
        assertEquals(3, persistent.surrenderedSpared)
    }
}
