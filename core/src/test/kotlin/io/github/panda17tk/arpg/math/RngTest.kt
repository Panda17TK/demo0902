package io.github.panda17tk.arpg.math

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RngTest {
    @Test fun `same seed produces same sequence`() {
        val a = Rng(42L); val b = Rng(42L)
        repeat(100) { assertEquals(a.nextFloat(), b.nextFloat(), 0f) }
    }
    @Test fun `different seeds diverge`() {
        val a = Rng(1L); val b = Rng(2L)
        val same = (0 until 100).count { a.nextFloat() == b.nextFloat() }
        assertTrue(same < 100, "sequences should differ")
    }
    @Test fun `nextFloat is in 0 until 1`() {
        val r = Rng(7L)
        repeat(1000) { val v = r.nextFloat(); assertTrue(v >= 0f && v < 1f, "out of range: $v") }
    }
    @Test fun `range returns within bounds`() {
        val r = Rng(7L)
        repeat(1000) { val v = r.range(5f, 10f); assertTrue(v >= 5f && v < 10f, "out of range: $v") }
    }
}
