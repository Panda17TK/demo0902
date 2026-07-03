package io.github.panda17tk.arpg.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TransitionFadeTest {
    @Test fun `start opens the OUT leg once and refuses re-entry`() {
        val f = TransitionFade(leg = 0.3f)
        assertTrue(f.start())
        assertFalse(f.start(), "a running fade must refuse a second start")
        assertTrue(f.blocksInput)
    }

    @Test fun `the transition fires exactly once, behind full black`() {
        val f = TransitionFade(leg = 0.3f)
        f.start()
        var fired = 0
        repeat(60) { if (f.update(1f / 60f)) fired++ }
        assertEquals(1, fired, "the OUT-complete signal must fire exactly once")
    }

    @Test fun `alpha rises through OUT and falls through IN back to idle`() {
        val f = TransitionFade(leg = 0.2f)
        assertEquals(0f, f.alpha)
        f.start()
        f.update(0.1f)
        assertEquals(0.5f, f.alpha, 0.01f) // halfway out
        f.update(0.1f)                     // OUT completes → IN begins at full black
        assertEquals(1f, f.alpha, 0.01f)
        f.update(0.1f)
        assertEquals(0.5f, f.alpha, 0.01f) // halfway back in
        f.update(0.1f)
        assertEquals(TransitionFade.Phase.NONE, f.phase)
        assertEquals(0f, f.alpha)
        assertFalse(f.blocksInput)
    }

    @Test fun `idle updates do nothing`() {
        val f = TransitionFade()
        repeat(10) { assertFalse(f.update(1f)) }
        assertEquals(0f, f.alpha)
    }
}
