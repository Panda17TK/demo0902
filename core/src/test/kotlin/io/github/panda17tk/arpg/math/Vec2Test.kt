package io.github.panda17tk.arpg.math

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Vec2Test {
    @Test fun `length of 3-4 vector is 5`() {
        assertEquals(5f, Vec2(3f, 4f).length(), 1e-5f)
    }
    @Test fun `normalized vector has unit length`() {
        assertEquals(1f, Vec2(3f, 4f).normalized().length(), 1e-5f)
    }
    @Test fun `normalizing zero vector returns zero`() {
        val n = Vec2(0f, 0f).normalized()
        assertEquals(0f, n.x, 1e-5f); assertEquals(0f, n.y, 1e-5f)
    }
    @Test fun `scale multiplies both components`() {
        val v = Vec2(2f, -3f) * 2f
        assertEquals(4f, v.x, 1e-5f); assertEquals(-6f, v.y, 1e-5f)
    }
}
