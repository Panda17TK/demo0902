package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CreatureAITest {
    private fun state(
        hp: Float, bravery: Float = 1f, intel: Float = 0f, canBeg: Boolean = false,
        canHide: Boolean = false, mercy: Float = 0f, threatened: Boolean = false, protect: Float = 0f,
    ) = CreatureAI.nextState(hp, bravery, intel, canBeg, canHide, mercy, threatened, protect)

    @Test fun `healthy creature stays hostile`() {
        assertEquals(CreatureState.Hostile, state(hp = 0.9f, bravery = 0.1f, canBeg = true))
    }

    @Test fun `brave creature never flees even when low`() {
        assertEquals(CreatureState.Hostile, state(hp = 0.1f, bravery = 1f))
    }

    @Test fun `cowardly low creature flees`() {
        assertEquals(CreatureState.Flee, state(hp = 0.2f, bravery = 0.2f))
    }

    @Test fun `cowardly creature near death begs when it can`() {
        assertEquals(CreatureState.Beg, state(hp = 0.1f, bravery = 0.2f, canBeg = true, mercy = 0.2f))
    }

    @Test fun `smart wounded creature hides`() {
        assertEquals(CreatureState.Hide, state(hp = 0.2f, bravery = 0.2f, intel = 0.8f, canHide = true))
    }

    @Test fun `protective creature guards a threatened ward even at full health`() {
        assertEquals(CreatureState.Protect, state(hp = 1f, threatened = true, protect = 0.8f))
    }

    @Test fun `protective creature guards rather than fleeing when low`() {
        assertEquals(CreatureState.Protect, state(hp = 0.1f, bravery = 0.1f, threatened = true, protect = 0.8f))
    }
}
