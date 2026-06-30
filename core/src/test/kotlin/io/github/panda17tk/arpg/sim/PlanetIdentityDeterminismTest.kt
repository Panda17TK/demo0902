package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlanetIdentityDeterminismTest {
    @Test fun `idFor is deterministic for a given seed and index`() {
        assertEquals(Planets.idFor(42L, 3), Planets.idFor(42L, 3))
    }

    @Test fun `different indices in a stage yield distinct ids`() {
        val ids = (0 until 8).map { Planets.idFor(42L, it) }
        assertEquals(ids.size, ids.toSet().size, "all planet ids in a stage must be distinct")
    }

    @Test fun `different seeds yield different ids for the same index`() {
        assertNotEquals(Planets.idFor(1L, 0), Planets.idFor(2L, 0))
    }

    @Test fun `place reproduces identical ids for the same seed`() {
        fun ids(seed: Long) = Planets.place(4000f, 4000f, 200f, 200f, 5, Rng(seed xor 0x91A2B3C4L), seed = seed).map { it.id }
        assertEquals(ids(7L), ids(7L))
    }

    @Test fun `planets within one placement have distinct ids`() {
        val planets = Planets.place(4000f, 4000f, 200f, 200f, 5, Rng(7L xor 0x91A2B3C4L), seed = 7L)
        assertTrue(planets.isNotEmpty())
        assertEquals(planets.size, planets.map { it.id }.toSet().size)
    }
}
