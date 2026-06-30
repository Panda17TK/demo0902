package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReturnVisitHudLineTest {
    @Test fun `a hostile memory greets coldly`() {
        assertEquals("この星はあなたを敵として覚えている", ReturnVisitLine.hudLine(PlanetSocietyState(hostility = 0.7f)))
    }

    @Test fun `a hostile memory carrying old wounds is flavoured`() {
        assertEquals("森は前の傷を覚えている", ReturnVisitLine.hudLine(PlanetSocietyState(hostility = 0.7f).also { it.childHarmed = true }))
    }

    @Test fun `an indebted memory greets warmly`() {
        assertEquals("この星はあなたへの借りを覚えている", ReturnVisitLine.hudLine(PlanetSocietyState(mercy = 0.6f)))
    }

    @Test fun `a guardian still owes you for a saved child`() {
        assertEquals("守護者はまだ借りを覚えている", ReturnVisitLine.hudLine(PlanetSocietyState(mercy = 0.6f).also { it.predatorKilledNearChild = true }))
    }
}
