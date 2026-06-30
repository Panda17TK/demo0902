package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VengefulChildHarmImpactTest {
    private fun ctx(t: PlanetTemperament, s: SacredThing) = PlanetContext(t, s, PlanetStorySeed.NONE)

    @Test fun `a vengeful world takes child-harm harder than a neutral one`() {
        val vengeful = PlanetSocietyState().also { it.onChildHarmed(ctx(PlanetTemperament.VENGEFUL, SacredThing.RUINS)) }
        val neutral = PlanetSocietyState().also { it.onChildHarmed(PlanetContext.NEUTRAL) }
        assertTrue(vengeful.hostility > neutral.hostility, "${vengeful.hostility} should exceed ${neutral.hostility}")
    }

    @Test fun `a neutral context preserves the original deltas (backward compatible)`() {
        assertEquals(0.3f, PlanetSocietyState().also { it.onChildHarmed() }.hostility, 1e-6f)
        assertEquals(0.8f, PlanetSocietyState().also { it.onChildKilled() }.hostility, 1e-6f)
        assertEquals(0.5f, PlanetSocietyState().also { it.onApexKilled() }.ecologicalDisruption, 1e-6f)
        assertEquals(0.4f, PlanetSocietyState().also { it.onPredatorRepelledNearChild() }.mercy, 1e-6f)
    }
}
