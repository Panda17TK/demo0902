package io.github.panda17tk.arpg.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UiButtonTest {
    private val b = UiButton(10f, 20f, 100f, 40f, "再開")

    @Test fun `contains a point inside`() {
        assertTrue(b.contains(50f, 40f))
    }

    @Test fun `rejects points outside on every side`() {
        assertFalse(b.contains(5f, 40f))    // left of x
        assertFalse(b.contains(200f, 40f))  // right of x+w
        assertFalse(b.contains(50f, 5f))    // below y
        assertFalse(b.contains(50f, 100f))  // above y+h
    }

    @Test fun `includes points exactly on the edges and corners`() {
        assertTrue(b.contains(10f, 20f))    // bottom-left corner
        assertTrue(b.contains(110f, 60f))   // top-right corner
        assertTrue(b.contains(10f, 40f))    // left edge
        assertTrue(b.contains(110f, 40f))   // right edge
    }

    @Test fun `center is the geometric middle of the rect`() {
        assertEquals(60f, b.centerX, 0.001f)
        assertEquals(40f, b.centerY, 0.001f)
    }
}
