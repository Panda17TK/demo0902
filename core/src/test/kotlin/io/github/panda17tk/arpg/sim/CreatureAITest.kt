package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CreatureAITest {
    private fun state(
        hp: Float, bravery: Float = 1f, intel: Float = 0f, canBeg: Boolean = false,
        canHide: Boolean = false, mercy: Float = 0f, threatened: Boolean = false, protect: Float = 0f,
        wardHurt: Boolean = false, playerNear: Boolean = false, canWarn: Boolean = false,
    ) = CreatureAI.nextState(
        hp, bravery, intel, canBeg, canHide, mercy, threatened, protect, wardHurt, playerNear, canWarn,
    )

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

    // --- Living Planets additions: warn, rally ---

    @Test fun `territorial creature warns before it fights`() {
        assertEquals(CreatureState.Warn, state(hp = 1f, canWarn = true, playerNear = true))
    }

    @Test fun `a provoked creature no longer warns`() {
        assertEquals(CreatureState.Hostile, state(hp = 1f, canWarn = false, playerNear = true))
    }

    @Test fun `warning needs the player in range`() {
        assertEquals(CreatureState.Hostile, state(hp = 1f, canWarn = true, playerNear = false))
    }

    @Test fun `a wounded ward rallies fierce protectors over flee or beg`() {
        assertEquals(
            CreatureState.Rally,
            state(hp = 0.1f, bravery = 0.1f, canBeg = true, mercy = 0.5f, protect = 0.8f, wardHurt = true),
        )
    }

    @Test fun `a soft creature does not rally for a wounded ward`() {
        assertEquals(CreatureState.Hostile, state(hp = 1f, protect = 0.2f, wardHurt = true))
    }

    @Test fun `guarding a crowded ward takes priority over warning`() {
        assertEquals(
            CreatureState.Protect,
            state(hp = 1f, threatened = true, protect = 0.8f, canWarn = true, playerNear = true),
        )
    }
}
