package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SocietyPressureTest {
    @Test fun `toPressure carries the gauges, the deed flags, and the planet's character`() {
        val soc = PlanetSocietyState(hostility = 0.7f, mercy = 0.3f, ecologicalDisruption = 0.4f).also {
            it.childHarmed = true; it.childKilled = true; it.predatorKilledNearChild = true; it.apexKilled = true
        }
        val ctx = PlanetContext(PlanetTemperament.VENGEFUL, SacredThing.CHILDREN, PlanetStorySeed.NONE)
        val p = soc.toPressure(ctx)
        assertEquals(0.7f, p.hostility, 1e-6f)
        assertEquals(0.3f, p.mercy, 1e-6f)
        assertEquals(0.4f, p.ecologicalDisruption, 1e-6f)
        assertTrue(p.childHarmed && p.childKilled && p.predatorKilledNearChild && p.apexKilled)
        assertEquals(PlanetTemperament.VENGEFUL, p.temperament)
        assertEquals(SacredThing.CHILDREN, p.sacredThing)
    }

    @Test fun `a blank society projects a blank pressure`() {
        val p = PlanetSocietyState().toPressure(PlanetContext.NEUTRAL)
        assertEquals(0f, p.hostility, 1e-6f)
        assertTrue(!p.childHarmed && !p.childKilled && !p.apexKilled)
    }
}
