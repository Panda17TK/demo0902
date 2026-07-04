package io.github.panda17tk.arpg.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PauseFlowTest {
    @Test fun `toggle opens the pause overlay from play`() {
        assertEquals(Overlay.PAUSE, PauseFlow.toggle(Overlay.NONE))
    }

    @Test fun `toggle closes the pause overlay back to play`() {
        assertEquals(Overlay.NONE, PauseFlow.toggle(Overlay.PAUSE))
    }

    @Test fun `toggle from the help screen returns to play`() {
        assertEquals(Overlay.NONE, PauseFlow.toggle(Overlay.HELP))
    }

    @Test fun `pause button indices map to their actions`() {
        assertEquals(PauseAction.RESUME, PauseFlow.action(0))
        assertEquals(PauseAction.RESTART, PauseFlow.action(1))
        assertEquals(PauseAction.HELP, PauseFlow.action(2))
    }

    @Test fun `out-of-range pause indices have no action`() {
        assertNull(PauseFlow.action(-1))
        assertNull(PauseFlow.action(5, hasMemory = false))
        assertNull(PauseFlow.action(6, hasMemory = true))
    }

    @Test fun `the memory entry only exists on a surface pause`() {
        // v2.53: the SIM entry slots in before the always-last FORGET.
        assertEquals(PauseAction.SIM, PauseFlow.action(3, hasMemory = false))
        assertEquals(PauseAction.FORGET, PauseFlow.action(4, hasMemory = false))
        assertEquals(PauseAction.MEMORY, PauseFlow.action(3, hasMemory = true))
        assertEquals(PauseAction.SIM, PauseFlow.action(4, hasMemory = true))
        assertEquals(PauseAction.FORGET, PauseFlow.action(5, hasMemory = true))
    }

    @Test fun `toggle from the memory screen returns to play`() {
        assertEquals(Overlay.NONE, PauseFlow.toggle(Overlay.MEMORY))
    }
}
