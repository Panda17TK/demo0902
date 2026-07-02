package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReturnVisitEffectsTest {
    @Test fun `a first visit is exactly neutral`() {
        assertEquals(SpawnTweaks.NEUTRAL, ReturnVisitEffects.spawnTweaks(SocietyPressure()))
    }

    @Test fun `below every gate stays neutral`() {
        val p = SocietyPressure(hostility = 0.59f, mercy = 0.49f, ecologicalDisruption = 0.49f)
        assertEquals(SpawnTweaks.NEUTRAL, ReturnVisitEffects.spawnTweaks(p))
    }

    @Test fun `a hostile world posts one guard, a vengeful one two`() {
        assertEquals(1, ReturnVisitEffects.spawnTweaks(SocietyPressure(hostility = 0.6f)).extraGuardsAtPad)
        val vengeful = SocietyPressure(hostility = 0.6f, temperament = PlanetTemperament.VENGEFUL)
        assertEquals(2, ReturnVisitEffects.spawnTweaks(vengeful).extraGuardsAtPad)
    }

    @Test fun `a grateful world calms the wild and yields richer spoils`() {
        val t = ReturnVisitEffects.spawnTweaks(SocietyPressure(mercy = 0.5f))
        assertTrue(t.fleeSuppressed)
        assertEquals(ReturnVisitEffects.BONUS_MATERIAL, t.bonusMaterialChance)
        assertEquals(0, t.extraGuardsAtPad)
    }

    @Test fun `a disrupted world thins the grazers and starves the hunters`() {
        val t = ReturnVisitEffects.spawnTweaks(SocietyPressure(ecologicalDisruption = 0.5f))
        assertEquals(ReturnVisitEffects.HERBIVORE_CUT, t.herbivoreMul)
        assertEquals(ReturnVisitEffects.PREDATOR_HUNGER, t.predatorStartHunger)
        assertFalse(t.fleeSuppressed)
    }

    @Test fun `feelings compose without cancelling`() {
        val p = SocietyPressure(hostility = 0.9f, mercy = 0.6f, ecologicalDisruption = 0.7f)
        val t = ReturnVisitEffects.spawnTweaks(p)
        assertEquals(1, t.extraGuardsAtPad)
        assertTrue(t.fleeSuppressed)
        assertEquals(ReturnVisitEffects.HERBIVORE_CUT, t.herbivoreMul)
    }
}
