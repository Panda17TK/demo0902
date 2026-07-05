package io.github.panda17tk.arpg.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.86 開幕バナー: rides in, holds, lets go — pure timing. */
class EventBannerTest {
    @Test fun `a banner rises, holds and expires on schedule`() {
        val b = EventBanner()
        assertFalse(b.active)
        assertEquals(0f, b.alpha())
        b.start("磁気嵐 — 敵は荒ぶり、星屑は多くこぼれる")
        assertTrue(b.active)
        b.update(EventBanner.RISE / 2f)
        assertTrue(b.alpha() in 0.3f..0.7f, "mid-rise is mid-alpha")
        b.update(EventBanner.RISE) // into the hold
        assertEquals(1f, b.alpha(), 1e-4f)
        b.update(EventBanner.LIFE) // way past the end
        assertFalse(b.active)
        assertEquals(0f, b.alpha())
    }

    @Test fun `the line slides in from the right and drifts left through the hold`() {
        val b = EventBanner()
        b.start("大群が接近している")
        val early = b.slide()
        assertTrue(early > EventBanner.SLIDE_IN * 0.8f, "starts off to the right")
        b.update(EventBanner.RISE + 0.01f)
        val held = b.slide()
        assertTrue(held <= 0f && held > -EventBanner.DRIFT, "drifts gently left while held")
    }

    @Test fun `a new line restarts the ride`() {
        val b = EventBanner()
        b.start("A"); b.update(EventBanner.LIFE + 1f)
        assertFalse(b.active)
        b.start("B")
        assertTrue(b.active)
        assertEquals("B", b.text)
    }
}
