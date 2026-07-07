package io.github.panda17tk.arpg.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.108 ナビ: the edge-marker math — visible POIs stay unmarked, off-screen ones pin to the rim. */
class EdgeMarkersTest {
    private val viewW = 400f; private val viewH = 400f
    private val hudW = 360f; private val hudH = 800f
    private val m = 30f

    @Test fun `a visible landmark needs no pointer`() {
        assertNull(EdgeMarkers.place(0f, 0f, viewW, viewH, hudW, hudH, m))
        assertNull(EdgeMarkers.place(190f, -190f, viewW, viewH, hudW, hudH, m))
    }

    @Test fun `an off-screen landmark pins to the matching rim`() {
        // Far east → the right rim, vertically centered.
        val east = EdgeMarkers.place(2000f, 0f, viewW, viewH, hudW, hudH, m)!!
        assertEquals(hudW - m, east.first, 0.01f)
        assertEquals(hudH / 2f, east.second, 0.01f)
        // World y-down: dyWorld > 0 means BELOW the keeper → the marker sits at the BOTTOM rim.
        val south = EdgeMarkers.place(0f, 2000f, viewW, viewH, hudW, hudH, m)!!
        assertEquals(m, south.second, 0.01f, "south pins to the bottom (y-up HUD)")
        val north = EdgeMarkers.place(0f, -2000f, viewW, viewH, hudW, hudH, m)!!
        assertEquals(hudH - m, north.second, 0.01f, "north pins to the top")
    }

    @Test fun `every marker stays inside the margin box`() {
        for (dx in listOf(-5000f, -300f, 300f, 5000f)) for (dy in listOf(-5000f, -300f, 300f, 5000f)) {
            val p = EdgeMarkers.place(dx, dy, viewW, viewH, hudW, hudH, m) ?: continue
            assertTrue(p.first in m..(hudW - m) && p.second in m..(hudH - m), "outside the box: $p for ($dx,$dy)")
        }
    }
}
