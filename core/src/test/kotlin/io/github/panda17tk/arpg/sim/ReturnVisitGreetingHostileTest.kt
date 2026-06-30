package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReturnVisitGreetingHostileTest {
    @Test fun `a remembered hostile planet returns a hostile greeting`() {
        assertEquals(SocietySpeechTrigger.ReturnVisitHostile, SocietySpeechLines.returnGreeting(PlanetSocietyState(hostility = 0.7f)))
    }

    @Test fun `hostility outweighs mercy in the greeting`() {
        assertEquals(
            SocietySpeechTrigger.ReturnVisitHostile,
            SocietySpeechLines.returnGreeting(PlanetSocietyState(hostility = 0.6f, mercy = 0.6f)),
        )
    }
}
