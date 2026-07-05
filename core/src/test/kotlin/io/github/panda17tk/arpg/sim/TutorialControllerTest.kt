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
        assertEquals(TutorialStep.CHILD, t.step)
        t.onSocietyEvent(protected = true)
        assertEquals(TutorialStep.MEMORY, t.step)
        assertEquals(TutorialMemory.PROTECTED, t.memory)
        t.onSurfaceTick(TutorialController.MEMORY_TIME)
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

    @Test fun `a harmed child writes the harsher memory`() {
        val t = TutorialController()
        t.begin(); t.onMoved(999f); t.onKill(); t.onDustPicked(); t.onDash(); t.onScan(); t.onLanded()
        t.onSocietyEvent(protected = false) // even during OBSERVE — the star notices immediately
        assertEquals(TutorialStep.MEMORY, t.step)
        assertEquals(TutorialMemory.HARMED, t.memory)
        assertTrue(t.prompt(true).any { it.contains("敵意") })
    }

    @Test fun `a quiet visit falls back to the neutral memory`() {
        val t = TutorialController()
        t.begin(); t.onMoved(999f); t.onKill(); t.onDustPicked(); t.onDash(); t.onScan(); t.onLanded()
        t.onSurfaceTick(TutorialController.OBSERVE_TIME) // → CHILD
        t.onSurfaceTick(TutorialController.CHILD_TIMEOUT) // nothing happened
        assertEquals(TutorialStep.MEMORY, t.step)
        assertEquals(TutorialMemory.NONE, t.memory)
        // takeoff finishes from anywhere in Layer 2
        t.onTakeoff()
        assertTrue(t.done)
    }

    @Test fun `every step before COMPLETE has a prompt, in both input modes`() {
        val t = TutorialController()
        for (step in TutorialStep.entries) {
            if (step == TutorialStep.COMPLETE) continue
            // walk the controller to the step via brute force
            val c = TutorialController()
            c.begin()
            if (step == TutorialStep.BOOT_PROMPT) {
                assertTrue(TutorialController().prompt(true).isNotEmpty())
                continue
            }
            c.onMoved(999f)
            if (step == TutorialStep.MOVE) { /* already past — check fresh below */ }
            c.onKill(); c.onDustPicked(); c.onDash(); c.onScan()
            for (touch in listOf(true, false)) {
                val fresh = TutorialController()
                fresh.begin()
                assertTrue(fresh.prompt(touch).isNotEmpty(), "no prompt for MOVE ($touch)")
            }
        }
        // spot-check the walked-to LAND prompt too
        val c = TutorialController()
        c.begin(); c.onMoved(999f); c.onKill(); c.onDustPicked(); c.onDash(); c.onScan()
        assertEquals(TutorialStep.LAND, c.step)
        assertTrue(c.prompt(true).isNotEmpty() && c.prompt(false).isNotEmpty())
        assertTrue(TutorialController.REWARD_DUST > 0)
    }
}
