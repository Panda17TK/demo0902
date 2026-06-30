package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The pure rules that drive AISystem's society-pressure nudges (the system wiring just applies these). */
class AISystemSocietyPressureTest {
    @Test fun `a very hostile world skips the warning`() {
        assertTrue(SocietyTuning.ai(SocietyPressure(hostility = 0.8f)).skipWarn)
    }

    @Test fun `a proud world warns first even when hostile`() {
        assertFalse(SocietyTuning.ai(SocietyPressure(hostility = 0.9f, temperament = PlanetTemperament.PROUD)).skipWarn)
    }

    @Test fun `a killed child makes guardians rally eagerly`() {
        assertTrue(SocietyTuning.ai(SocietyPressure(childKilled = true)).rallyEager)
    }

    @Test fun `a vengeful world rallies on mere child harm, a gentle one does not`() {
        assertTrue(SocietyTuning.ai(SocietyPressure(childHarmed = true, temperament = PlanetTemperament.VENGEFUL)).rallyEager)
        assertFalse(SocietyTuning.ai(SocietyPressure(childHarmed = true, temperament = PlanetTemperament.GENTLE)).rallyEager)
    }

    @Test fun `high mercy eases creatures toward begging, more so on a gentle world`() {
        val gentle = SocietyTuning.ai(SocietyPressure(mercy = 0.8f, temperament = PlanetTemperament.GENTLE)).mercyBoost
        val plain = SocietyTuning.ai(SocietyPressure(mercy = 0.8f, temperament = PlanetTemperament.ANCIENT)).mercyBoost
        assertTrue(plain > 0f && gentle > plain)
    }

    @Test fun `driving a predator off a child earns the tribe's gratitude`() {
        assertTrue(SocietyTuning.ai(SocietyPressure(predatorKilledNearChild = true)).gratitude)
    }

    @Test fun `a blank pressure is entirely inert`() {
        val a = SocietyTuning.ai(SocietyPressure())
        assertFalse(a.skipWarn); assertFalse(a.rallyEager); assertFalse(a.gratitude)
        assertEquals(0f, a.mercyBoost, 1e-6f)
    }
}
