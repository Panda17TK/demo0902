package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.60 起動診断: the layer-1 state machine — order, gating, skipping, prompts. */
class TutorialControllerTest {
    @Test fun `the happy path walks every step in order`() {
        val t = TutorialController()
        assertEquals(TutorialStep.BOOT_PROMPT, t.step)
        t.begin(); assertEquals(TutorialStep.MOVE, t.step)
        t.onMoved(TutorialController.MOVE_GOAL / 2f)
        assertEquals(TutorialStep.MOVE, t.step) // still drifting
        t.onMoved(TutorialController.MOVE_GOAL)
        assertEquals(TutorialStep.SHOOT, t.step)
        t.onKill(); assertEquals(TutorialStep.PICKUP_DUST, t.step)
        t.onDustPicked(); assertEquals(TutorialStep.DASH, t.step)
        t.onDash(); assertEquals(TutorialStep.FIND_PLANET, t.step)
        t.onScan(); assertEquals(TutorialStep.LAND, t.step)
        // v2.61 Layer 2: touchdown opens the surface beats instead of ending the diagnostic.
        t.onLanded(); assertEquals(TutorialStep.OBSERVE, t.step)
        t.onSurfaceTick(TutorialController.OBSERVE_TIME)
        assertEquals(TutorialStep.RETURN_PAD, t.step)
        t.onTakeoff()
        assertTrue(t.done)
        assertFalse(t.skipped)
    }

    @Test fun `events out of order do not advance the wrong step`() {
        val t = TutorialController()
        t.begin()
        t.onKill(); t.onDustPicked(); t.onDash(); t.onScan()
        assertEquals(TutorialStep.MOVE, t.step) // only movement counts right now
    }

    @Test fun `skip works from any step and marks itself`() {
        val t = TutorialController()
        t.begin(); t.onMoved(999f)
        t.skip()
        assertTrue(t.done)
        assertTrue(t.skipped)
        assertTrue(t.completionToast().contains("スキップ"))
    }

    @Test fun `landing early still opens the surface beats`() {
        val t = TutorialController()
        t.begin(); t.onMoved(999f); t.onKill(); t.onDustPicked(); t.onDash()
        t.onLanded() // landed straight from FIND_PLANET without a latched scan
        assertEquals(TutorialStep.OBSERVE, t.step)
    }

    @Test fun `takeoff completes the diagnostic from anywhere in Layer 2`() { // v2.146: the child beat is gone
        val t = TutorialController()
        t.begin(); t.onMoved(999f); t.onKill(); t.onDustPicked(); t.onDash(); t.onScan(); t.onLanded()
        assertEquals(TutorialStep.OBSERVE, t.step)
        t.onTakeoff() // leaving mid-observation still finishes — the diagnostic never traps
        assertTrue(t.done)
    }

    @Test fun `every step before COMPLETE has a prompt, in both input modes`() {
        // v2.151: the old loop never advanced past MOVE — this walk asserts EVERY live step.
        for (touch in listOf(true, false)) {
            val c = TutorialController()
            fun check() = assertTrue(c.prompt(touch).isNotEmpty(), "no prompt for ${c.step} ($touch)")
            check()                                             // BOOT_PROMPT
            c.begin(); check()                                  // MOVE
            c.onMoved(999f); check()                            // SHOOT
            c.onKill(); check()                                 // PICKUP_DUST
            c.onDustPicked(); check()                           // DASH
            c.onDash(); check()                                 // FIND_PLANET
            c.onScan(); check()                                 // LAND
            c.onLanded(); check()                               // OBSERVE
            c.onSurfaceTick(TutorialController.OBSERVE_TIME); check() // RETURN_PAD
            assertEquals(TutorialStep.RETURN_PAD, c.step)
        }
        assertTrue(TutorialController.REWARD_DUST > 0)
    }
}
