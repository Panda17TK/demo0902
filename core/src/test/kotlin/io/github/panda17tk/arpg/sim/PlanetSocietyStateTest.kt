package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlanetSocietyStateTest {
    @Test fun `harming a child raises hostility`() {
        val s = PlanetSocietyState(); s.onChildHarmed()
        assertTrue(s.childHarmed && s.hostility > 0f, "hostility=${s.hostility}")
    }

    @Test fun `killing a child raises hostility more than merely harming one`() {
        val harmed = PlanetSocietyState().apply { onChildHarmed() }
        val killed = PlanetSocietyState().apply { onChildKilled() }
        assertTrue(killed.childKilled)
        assertTrue(killed.hostility > harmed.hostility, "${killed.hostility} vs ${harmed.hostility}")
    }

    @Test fun `repelling a predator near a child raises mercy`() {
        val s = PlanetSocietyState(); s.onPredatorRepelledNearChild()
        assertTrue(s.predatorKilledNearChild && s.mercy > 0f, "mercy=${s.mercy}")
    }

    @Test fun `killing a hatchling raises ecological disruption`() {
        val s = PlanetSocietyState(); s.onHatchlingKilled()
        assertTrue(s.hatchlingKilled && s.ecologicalDisruption > 0f)
    }

    @Test fun `killing the apex shakes the ecosystem the hardest`() {
        val hatch = PlanetSocietyState().apply { onHatchlingKilled() }
        val apex = PlanetSocietyState().apply { onApexKilled() }
        assertTrue(apex.apexKilled)
        assertTrue(apex.ecologicalDisruption > hatch.ecologicalDisruption)
    }

    @Test fun `the gauges never exceed one`() {
        val s = PlanetSocietyState()
        repeat(10) { s.onChildKilled() }
        assertTrue(s.hostility <= 1f, "hostility=${s.hostility}")
    }
}
