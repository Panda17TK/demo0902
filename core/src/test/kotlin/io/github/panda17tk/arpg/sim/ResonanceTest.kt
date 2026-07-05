package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.87 記憶核の共鳴: a pure clock → pulse ring; lights what it passes, rests between. */
class ResonanceTest {
    @Test fun `the pulse sweeps out, then the core rests`() {
        assertEquals(0f, Resonance.radius(0f))
        val mid = Resonance.radius(Resonance.SWEEP / 2f)!!
        assertTrue(mid > 0f && mid < Resonance.RANGE)
        assertEquals(Resonance.RANGE, Resonance.radius(Resonance.SWEEP - 1e-4f)!!, 1f)
        assertNull(Resonance.radius(Resonance.SWEEP + 0.1f), "between pulses the core rests")
        assertNull(Resonance.radius(Resonance.PERIOD - 0.1f))
    }

    @Test fun `the pulse repeats every period`() {
        val a = Resonance.radius(1.0f)!!
        val b = Resonance.radius(1.0f + Resonance.PERIOD)!!
        assertEquals(a, b, 0.001f)
    }

    @Test fun `a point is lit exactly while the ring passes it`() {
        val d = Resonance.RANGE / 2f
        val tAtPoint = Resonance.SWEEP / 2f // the ring reaches half range at half sweep
        assertTrue(Resonance.lit(tAtPoint, d))
        assertFalse(Resonance.lit(tAtPoint, d + Resonance.BAND * 3f), "far ahead of the ring stays dark")
        assertFalse(Resonance.lit(Resonance.SWEEP + 1f, d), "nothing is lit while the core rests")
    }
}
