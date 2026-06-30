package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Simulates GameScreen's land → (ecology events) → takeoff → re-land flow against the pure memory book. */
class ReturnVisitMemoryTest {
    @Test fun `harm done on a first visit is remembered on the next landing`() {
        val book = PlanetMemoryBook()
        val id = Planets.idFor(123L, 0)

        // First landing: seed from memory (blank), the player wounds a child, then takes off.
        val firstVisit = book.recall(id)
        firstVisit.onChildHarmed()
        book.remember(id, firstVisit)

        // Re-land on the SAME planet: the society remembers.
        val secondVisit = book.recall(id)
        assertTrue(secondVisit.childHarmed)
        assertTrue(secondVisit.hostility > 0f)
    }

    @Test fun `a different planet keeps its own, separate memory`() {
        val book = PlanetMemoryBook()
        val harmed = Planets.idFor(123L, 0)
        val pristine = Planets.idFor(123L, 1)
        book.recall(harmed).also { it.onChildKilled() }.let { book.remember(harmed, it) }

        assertTrue(book.recall(harmed).childKilled)
        assertFalse(book.recall(pristine).childKilled, "another planet must not inherit the deed")
    }

    @Test fun `gauges persist and do not double-count across repeated visits`() {
        val book = PlanetMemoryBook()
        val id = Planets.idFor(9L, 2)
        // Visit 1: apex killed (disruption climbs).
        book.recall(id).also { it.onApexKilled() }.let { book.remember(id, it) }
        val afterFirst = book.recall(id).ecologicalDisruption
        // Visit 2: seeded from memory, no new events → disruption unchanged (not stacked).
        book.recall(id).let { book.remember(id, it) }
        assertTrue(afterFirst > 0f)
        assertEquals(afterFirst, book.recall(id).ecologicalDisruption, 1e-6f)
    }
}
