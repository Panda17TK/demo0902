package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MemoryBookNoAliasingRegressionTest {
    @Test fun `recall hands out independent copies that never bleed into the book or each other`() {
        val book = PlanetMemoryBook()
        val id = Planets.idFor(7L, 0)
        book.remember(id, PlanetSocietyState().also { it.onApexKilled() })

        val a = book.recall(id)
        val b = book.recall(id)
        a.onChildKilled() // mutate one recalled visit

        assertFalse(b.childKilled, "two recalls must not share state")
        assertFalse(book.recall(id).childKilled, "the book must not be mutated by a recalled visit")
        assertTrue(book.recall(id).apexKilled, "the original remembered deed persists")
    }
}
