package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TribesTest {
    @Test fun `tribeOf returns the nearest centre`() {
        val t = Tribes(listOf(floatArrayOf(0f, 0f), floatArrayOf(100f, 0f)), arrayOf(BooleanArray(2), BooleanArray(2)))
        assertEquals(0, t.tribeOf(10f, 0f))
        assertEquals(1, t.tribeOf(90f, 0f))
    }

    @Test fun `a tribe is never hostile to itself`() {
        val t = Tribes.build(4, 100f, 100f, 1f, Rng(1L))
        assertFalse(t.areHostile(2, 2))
    }

    @Test fun `hostility is symmetric`() {
        val t = Tribes.build(5, 100f, 100f, 0.5f, Rng(9L))
        for (a in 0 until t.count) for (b in 0 until t.count) {
            assertEquals(t.areHostile(a, b), t.areHostile(b, a))
        }
    }

    @Test fun `chance 1 makes every distinct pair hostile`() {
        val t = Tribes.build(4, 100f, 100f, 1f, Rng(2L))
        assertTrue(t.areHostile(0, 1))
        assertTrue(t.areHostile(1, 2))
        assertTrue(t.areHostile(0, 3))
    }

    @Test fun `chance 0 makes no pair hostile`() {
        val t = Tribes.build(4, 100f, 100f, 0f, Rng(3L))
        for (a in 0 until 4) for (b in 0 until 4) assertFalse(t.areHostile(a, b))
    }
}
