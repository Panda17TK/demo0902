package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.config.WildState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WildAIFleeSuppressionTest {
    private fun state(role: WildRole, fleeSuppressed: Boolean, predatorNear: Boolean = false) = WildAI.nextState(
        role, hpFrac = 1f, playerDist = 50f, // well inside FLEE_DIST
        predatorNear = predatorNear, preyNear = false, nestThreatened = false, herdSeparated = false,
        hunger = 0f, fear = 0.9f, fleeSuppressed = fleeSuppressed,
    )

    @Test fun `a herd normally flees the close player`() {
        assertEquals(WildState.Flee, state(WildRole.HERD, fleeSuppressed = false))
    }

    @Test fun `a grateful world's herd stands its ground`() {
        assertEquals(WildState.Graze, state(WildRole.HERD, fleeSuppressed = true))
    }

    @Test fun `a grateful world's hatchling keeps wandering`() {
        assertEquals(WildState.Wander, state(WildRole.HATCHLING, fleeSuppressed = true))
    }

    @Test fun `a predator still spooks a calmed herd`() {
        assertEquals(WildState.Flee, state(WildRole.HERD, fleeSuppressed = true, predatorNear = true))
    }

    @Test fun `prey keeps its usual caution even on a grateful world`() {
        assertEquals(WildState.Flee, state(WildRole.PREY, fleeSuppressed = true))
    }
}
