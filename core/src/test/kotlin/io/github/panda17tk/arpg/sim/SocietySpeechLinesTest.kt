package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SocietySpeechLinesTest {
    @Test fun `every trigger has at least one line`() {
        for (t in SocietySpeechTrigger.values()) assertNotNull(SocietySpeechLines.pick(t, 0), "no line for $t")
    }

    @Test fun `pick is deterministic and wraps the salt`() {
        val t = SocietySpeechTrigger.ChildKilled
        assertEquals(SocietySpeechLines.pick(t, 1), SocietySpeechLines.pick(t, 1))
        assertEquals(SocietySpeechLines.pick(t, 0), SocietySpeechLines.pick(t, 4)) // 4 lines → salt 4 wraps to 0 (v2.51)
    }

    @Test fun `a negative salt still picks a line`() {
        assertNotNull(SocietySpeechLines.pick(SocietySpeechTrigger.MercyHigh, -7))
    }
}
