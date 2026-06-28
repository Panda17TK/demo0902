package io.github.panda17tk.arpg.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NumbersTest {
    @Test
    fun `clamp returns value inside range unchanged`() {
        assertEquals(5.0, clamp(5.0, 0.0, 10.0, -1.0))
    }

    @Test
    fun `clamp pins below the lower bound`() {
        assertEquals(0.0, clamp(-3.0, 0.0, 10.0, -1.0))
    }

    @Test
    fun `clamp pins above the upper bound`() {
        assertEquals(10.0, clamp(42.0, 0.0, 10.0, -1.0))
    }

    @Test
    fun `clamp returns default for non-finite input`() {
        assertEquals(-1.0, clamp(Double.NaN, 0.0, 10.0, -1.0))
    }

    @Test
    fun `clamp returns default for infinite input`() {
        assertEquals(-1.0, clamp(Double.POSITIVE_INFINITY, 0.0, 10.0, -1.0))
    }
}
