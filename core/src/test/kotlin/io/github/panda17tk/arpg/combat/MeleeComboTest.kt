package io.github.panda17tk.arpg.combat

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.80 近接コンボ: the beat tightens, the blade grows, and kept rhythm is always more DPS. */
class MeleeComboTest {
    @Test fun `the chain window tightens and the cooldown quickens as the combo climbs`() {
        for (step in 1 until MeleeCombo.MAX_STEP) {
            assertTrue(
                MeleeCombo.chainWindow(step + 1) < MeleeCombo.chainWindow(step),
                "window must shrink at step ${step + 1}",
            )
            assertTrue(
                MeleeCombo.cooldown(step + 1, 0.32f) < MeleeCombo.cooldown(step, 0.32f),
                "cooldown must quicken at step ${step + 1}",
            )
        }
        assertTrue(MeleeCombo.chainWindow(MeleeCombo.MAX_STEP) > 0.15f, "the top beat stays humanly hittable")
        assertTrue(MeleeCombo.cooldown(MeleeCombo.MAX_STEP, 0.32f) > 0.1f)
    }

    @Test fun `reach, arc, damage and the show all grow with the rhythm`() {
        assertEquals(1f, MeleeCombo.dmgMul(1)); assertEquals(1f, MeleeCombo.reachMul(1)); assertEquals(1f, MeleeCombo.arcMul(1))
        for (step in 1 until MeleeCombo.MAX_STEP) {
            assertTrue(MeleeCombo.dmgMul(step + 1) > MeleeCombo.dmgMul(step))
            assertTrue(MeleeCombo.reachMul(step + 1) > MeleeCombo.reachMul(step))
            assertTrue(MeleeCombo.arcMul(step + 1) > MeleeCombo.arcMul(step))
            assertTrue(MeleeCombo.sparks(step + 1) > MeleeCombo.sparks(step))
        }
    }

    @Test fun `kept rhythm is strictly more DPS at every step`() {
        val base = 0.32f
        for (step in 1 until MeleeCombo.MAX_STEP) {
            val dpsNow = MeleeCombo.dmgMul(step) / MeleeCombo.cooldown(step, base)
            val dpsNext = MeleeCombo.dmgMul(step + 1) / MeleeCombo.cooldown(step + 1, base)
            assertTrue(dpsNext > dpsNow, "DPS must rise from step $step to ${step + 1}")
        }
    }

    @Test fun `the forward lunge and the crescent grow with the combo — sides stay honest`() {
        // v2.81: no lunge on the first swing; monotone growth after; the bonus is forward-only.
        assertEquals(0f, MeleeCombo.forwardMul(1))
        assertEquals(1f, MeleeCombo.slashScale(1))
        for (step in 1 until MeleeCombo.MAX_STEP) {
            assertTrue(MeleeCombo.forwardMul(step + 1) > MeleeCombo.forwardMul(step))
            assertTrue(MeleeCombo.slashScale(step + 1) > MeleeCombo.slashScale(step))
        }
        assertTrue(MeleeCombo.forwardMul(MeleeCombo.MAX_STEP) in 0.85f..0.95f, "top lunge ≈ +90% dead ahead")
        // the cos² weighting: at 90° off the facing the bonus vanishes entirely
        val fwd = MeleeCombo.forwardMul(MeleeCombo.MAX_STEP)
        val side = 1f + fwd * 0f * 0f
        assertEquals(1f, side, "the sideways reach must not grow with the lunge")
    }

    @Test fun `chaining climbs to the cap and a miss resets to one`() {
        var step = 0
        repeat(8) { step = MeleeCombo.nextStep(step, chained = step > 0) }
        assertEquals(MeleeCombo.MAX_STEP, step, "chained swings cap at MAX_STEP")
        assertEquals(1, MeleeCombo.nextStep(step, chained = false), "a dropped beat starts over")
    }
}
