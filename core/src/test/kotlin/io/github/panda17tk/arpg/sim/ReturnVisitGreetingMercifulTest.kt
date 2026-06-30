package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReturnVisitGreetingMercifulTest {
    @Test fun `a remembered indebted planet returns a merciful greeting`() {
        assertEquals(SocietySpeechTrigger.ReturnVisitMerciful, SocietySpeechLines.returnGreeting(PlanetSocietyState(mercy = 0.6f)))
    }
}
