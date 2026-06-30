package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReturnVisitDoesNothingForFirstVisitTest {
    @Test fun `a blank (first-visit) society yields no greeting`() {
        assertNull(SocietySpeechLines.returnGreeting(PlanetSocietyState()))
        assertNull(ReturnVisitLine.hudLine(PlanetSocietyState()))
    }

    @Test fun `a faintly remembered planet stays below the greeting threshold`() {
        assertNull(ReturnVisitLine.hudLine(PlanetSocietyState(hostility = 0.1f, mercy = 0.1f)))
    }
}
