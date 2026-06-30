package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SocietyImpactTest {
    private fun ctx(t: PlanetTemperament, s: SacredThing) = PlanetContext(t, s, PlanetStorySeed.NONE)

    @Test fun `a vengeful world takes child-harm harder than a gentle one`() {
        val vengeful = SocietyImpact.childHarmed(1f, ctx(PlanetTemperament.VENGEFUL, SacredThing.RUINS))
        val gentle = SocietyImpact.childHarmed(1f, ctx(PlanetTemperament.GENTLE, SacredThing.RUINS))
        assertTrue(vengeful > gentle, "$vengeful should exceed $gentle")
    }

    @Test fun `striking what the planet holds sacred amplifies the impact`() {
        val sacredChild = SocietyImpact.childKilled(1f, ctx(PlanetTemperament.PROUD, SacredThing.CHILDREN))
        val plain = SocietyImpact.childKilled(1f, ctx(PlanetTemperament.PROUD, SacredThing.RUINS))
        assertTrue(sacredChild > plain)
    }

    @Test fun `an apex-sacred world reacts harder to an apex kill`() {
        val sacredApex = SocietyImpact.apexKilled(1f, ctx(PlanetTemperament.SILENT, SacredThing.APEX))
        val plain = SocietyImpact.apexKilled(1f, ctx(PlanetTemperament.SILENT, SacredThing.RUINS))
        assertTrue(sacredApex > plain)
    }

    @Test fun `gentle worlds reward mercy more than vengeful ones`() {
        val gentle = SocietyImpact.predatorRepelled(1f, ctx(PlanetTemperament.GENTLE, SacredThing.RUINS))
        val vengeful = SocietyImpact.predatorRepelled(1f, ctx(PlanetTemperament.VENGEFUL, SacredThing.RUINS))
        assertTrue(gentle > vengeful)
    }

    @Test fun `a neutral temperament with no sacred match leaves the base unchanged`() {
        assertEquals(1f, SocietyImpact.childHarmed(1f, ctx(PlanetTemperament.ANCIENT, SacredThing.RUINS)), 1e-6f)
    }
}
