package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SpeechMemoryTriggerTest {
    @Test fun `a killed child outranks every other memory`() {
        val s = PlanetSocietyState(hostility = 1f).also { it.childKilled = true; it.apexKilled = true }
        assertEquals(SocietySpeechTrigger.ChildKilled, SocietySpeechLines.triggerFor(s))
    }

    @Test fun `a slain apex is voiced when no child was killed`() {
        assertEquals(SocietySpeechTrigger.ApexKilled, SocietySpeechLines.triggerFor(PlanetSocietyState().also { it.apexKilled = true }))
    }

    @Test fun `a destroyed nest is voiced (hatchling or nest mother)`() {
        assertEquals(SocietySpeechTrigger.NestDestroyed, SocietySpeechLines.triggerFor(PlanetSocietyState().also { it.hatchlingKilled = true }))
        assertEquals(SocietySpeechTrigger.NestDestroyed, SocietySpeechLines.triggerFor(PlanetSocietyState().also { it.nestMotherKilled = true }))
    }

    @Test fun `repelling a predator near a child is voiced kindly`() {
        assertEquals(SocietySpeechTrigger.PredatorRepelled, SocietySpeechLines.triggerFor(PlanetSocietyState().also { it.predatorKilledNearChild = true }))
    }

    @Test fun `a taken relic is voiced`() {
        assertEquals(SocietySpeechTrigger.RelicTaken, SocietySpeechLines.triggerFor(PlanetSocietyState().also { it.relicClaimed = true }))
    }

    @Test fun `high gauges are voiced only when no specific deed dominates`() {
        assertEquals(SocietySpeechTrigger.HostilityHigh, SocietySpeechLines.triggerFor(PlanetSocietyState(hostility = 0.6f)))
        assertEquals(SocietySpeechTrigger.MercyHigh, SocietySpeechLines.triggerFor(PlanetSocietyState(mercy = 0.6f)))
    }

    @Test fun `a blank society has nothing pointed to say`() {
        assertNull(SocietySpeechLines.triggerFor(PlanetSocietyState()))
    }
}
