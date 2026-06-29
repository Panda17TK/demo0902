package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SpeechLinesTest {
    @Test fun `pick returns a line from the trigger set`() {
        val s = SpeechLines.pick(SpeechLines.Trigger.Beg, 0)
        assertNotNull(s)
        assertTrue(s!!.isNotEmpty())
    }

    @Test fun `pick is deterministic for the same salt`() {
        assertEquals(SpeechLines.pick(SpeechLines.Trigger.Flee, 5), SpeechLines.pick(SpeechLines.Trigger.Flee, 5))
    }

    @Test fun `pick stays in range for any salt`() {
        for (s in intArrayOf(-7, 0, 1, 100, 999)) assertNotNull(SpeechLines.pick(SpeechLines.Trigger.Warn, s))
    }

    @Test fun `forState maps break states to triggers and silent states to null`() {
        assertEquals(SpeechLines.Trigger.Beg, SpeechLines.forState(CreatureState.Beg))
        assertEquals(SpeechLines.Trigger.Flee, SpeechLines.forState(CreatureState.Flee))
        assertNull(SpeechLines.forState(CreatureState.Hostile))
    }
}
