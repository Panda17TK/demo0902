package io.github.panda17tk.arpg.input

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TouchButtonsTest {
    @Test fun `by default only dash, melee and weapon are shown`() {
        assertEquals(
            setOf(TouchButton.DASH, TouchButton.MELEE, TouchButton.WEAPON),
            TouchButtons.visible(blocks = 0, mag = 30, magSize = 30, canLand = false),
        )
    }

    @Test fun `reload appears only when the magazine is not full`() {
        assertTrue(TouchButton.RELOAD in TouchButtons.visible(blocks = 0, mag = 5, magSize = 30, canLand = false))
    }

    @Test fun `wall appears only when materials are available`() {
        assertTrue(TouchButton.WALL in TouchButtons.visible(blocks = 3, mag = 30, magSize = 30, canLand = false))
    }

    @Test fun `an infinite-magazine weapon never shows reload`() {
        assertFalse(TouchButton.RELOAD in TouchButtons.visible(blocks = 0, mag = 0, magSize = null, canLand = false))
    }

    @Test fun `a full magazine never shows reload`() {
        assertFalse(TouchButton.RELOAD in TouchButtons.visible(blocks = 0, mag = 30, magSize = 30, canLand = false))
    }

    @Test fun `land appears only when landing is possible`() {
        assertTrue(TouchButton.LAND in TouchButtons.visible(blocks = 0, mag = 30, magSize = 30, canLand = true))
        assertFalse(TouchButton.LAND in TouchButtons.visible(blocks = 0, mag = 30, magSize = 30, canLand = false))
    }
}
