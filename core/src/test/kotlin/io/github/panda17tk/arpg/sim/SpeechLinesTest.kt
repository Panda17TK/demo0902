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

    @Test fun `every trigger has at least one line`() {
        for (trig in SpeechLines.Trigger.values()) {
            val line = SpeechLines.pick(trig, 0)
            assertNotNull(line, "no line for $trig")
            assertTrue(line!!.isNotEmpty(), "blank line for $trig")
        }
    }

    @Test fun `forState maps break and social states to triggers`() {
        assertEquals(SpeechLines.Trigger.Warn, SpeechLines.forState(CreatureState.Warn))
        assertEquals(SpeechLines.Trigger.Aggro, SpeechLines.forState(CreatureState.Hostile))
        assertEquals(SpeechLines.Trigger.Beg, SpeechLines.forState(CreatureState.Beg))
        assertEquals(SpeechLines.Trigger.Flee, SpeechLines.forState(CreatureState.Flee))
        assertEquals(SpeechLines.Trigger.Rally, SpeechLines.forState(CreatureState.Rally))
        assertEquals(SpeechLines.Trigger.ProtectChild, SpeechLines.forState(CreatureState.Protect))
        assertEquals(SpeechLines.Trigger.Surrender, SpeechLines.forState(CreatureState.Surrender))
    }

    @Test fun `forState is total over every creature state and stays silent for ignore`() {
        for (st in CreatureState.values()) SpeechLines.forState(st) // must not throw for any state
        assertNull(SpeechLines.forState(CreatureState.Ignore))
    }
}
