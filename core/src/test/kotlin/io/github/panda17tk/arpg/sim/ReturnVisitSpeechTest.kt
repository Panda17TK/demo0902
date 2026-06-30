package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReturnVisitSpeechTest {
    @Test fun `a remembered hostile planet greets the returning player coldly`() {
        assertEquals(SocietySpeechTrigger.ReturnVisitHostile, SocietySpeechLines.returnGreeting(PlanetSocietyState(hostility = 0.6f)))
    }

    @Test fun `a remembered indebted planet greets warmly`() {
        assertEquals(SocietySpeechTrigger.ReturnVisitMerciful, SocietySpeechLines.returnGreeting(PlanetSocietyState(mercy = 0.6f)))
    }

    @Test fun `hostility wins the greeting when both feelings run high`() {
        assertEquals(SocietySpeechTrigger.ReturnVisitHostile, SocietySpeechLines.returnGreeting(PlanetSocietyState(hostility = 0.6f, mercy = 0.6f)))
    }

    @Test fun `a faintly remembered planet gives no special greeting`() {
        assertNull(SocietySpeechLines.returnGreeting(PlanetSocietyState(hostility = 0.1f)))
    }

    @Test fun `both return greetings have lines`() {
        assertNotNull(SocietySpeechLines.pick(SocietySpeechTrigger.ReturnVisitHostile, 0))
        assertNotNull(SocietySpeechLines.pick(SocietySpeechTrigger.ReturnVisitMerciful, 0))
    }
}
