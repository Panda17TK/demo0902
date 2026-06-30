package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Wiring checks for the space-drive movement. These read only stamina (independent of gravity, walls
 * and collision), so they pin the InputState→MovementSystem→SpaceDrive plumbing without flaking on the
 * stage layout: a held dash button drains hard, a stick shoved to the rim barely sips, a gentle push is free.
 */
class SpaceMovementTest {
    /** Run [frames] ticks of a seed-1 (space) world after [setup] tweaks the input; return (staminaLeft, staminaMax). */
    private fun staminaAfter(frames: Int, setup: (InputState) -> Unit): Pair<Float, Float> {
        val input = InputState()
        val gw = WorldFactory.create(input, seed = 1L)
        val max = with(gw.world) { gw.player[Stamina].let { it.value = it.max; it.overheat = false; it.max } }
        setup(input)
        repeat(frames) { gw.world.update(1f / 60f) }
        val left = with(gw.world) { gw.player[Stamina].value }
        return left to max
    }

    @Test fun `a button dash drains stamina far faster than a stick dash`() {
        val (buttonLeft, max) = staminaAfter(15) { it.right = true; it.dash = true }   // moving + dash button
        val (stickLeft, _) = staminaAfter(15) { it.right = true; it.moveMag = 1f }      // stick shoved to the rim
        assertTrue(buttonLeft < stickLeft - 5f, "button dash should drain more: button=$buttonLeft stick=$stickLeft")
        assertTrue(stickLeft > max * 0.9f, "a stick dash should barely sip stamina: $stickLeft of $max")
    }

    @Test fun `a gentle stick push walks for free`() {
        val (left, max) = staminaAfter(15) { it.right = true; it.moveMag = 0.5f } // below the stick-dash threshold
        assertTrue(left >= max - 0.01f, "walking shouldn't cost stamina: $left of $max")
    }
}
