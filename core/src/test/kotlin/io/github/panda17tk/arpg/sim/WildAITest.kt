package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.config.WildState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WildAITest {
    private fun state(
        role: WildRole,
        hpFrac: Float = 1f,
        playerDist: Float = 9999f,
        predatorNear: Boolean = false,
        preyNear: Boolean = false,
        nestThreatened: Boolean = false,
        herdSeparated: Boolean = false,
        hunger: Float = 0f,
        fear: Float = 0.5f,
    ) = WildAI.nextState(role, hpFrac, playerDist, predatorNear, preyNear, nestThreatened, herdSeparated, hunger, fear)

    @Test fun `prey flees a nearby predator`() {
        assertEquals(WildState.Flee, state(WildRole.PREY, predatorNear = true))
    }

    @Test fun `undisturbed prey just grazes`() {
        assertEquals(WildState.Graze, state(WildRole.PREY))
    }

    @Test fun `a herd regroups when separated`() {
        assertEquals(WildState.Herd, state(WildRole.HERD, herdSeparated = true))
    }

    @Test fun `a predator hunts nearby prey when hungry`() {
        assertEquals(WildState.Chase, state(WildRole.PREDATOR, preyNear = true, hunger = 0.9f))
    }

    @Test fun `a fed predator wanders rather than hunting`() {
        assertEquals(WildState.Wander, state(WildRole.PREDATOR, hunger = 0f))
    }

    @Test fun `an apex threatens an intruder in its territory`() {
        // inside the territory but beyond strike range → hold and threaten, not charge
        assertEquals(WildState.Threaten, state(WildRole.APEX, playerDist = 200f))
    }

    @Test fun `an apex runs down an intruder at close range`() {
        assertEquals(WildState.Chase, state(WildRole.APEX, playerDist = 80f))
    }

    @Test fun `a nest guard defends a threatened nest`() {
        assertEquals(WildState.GuardNest, state(WildRole.NEST_GUARD, nestThreatened = true))
    }

    @Test fun `a hatchling always bolts from a close player`() {
        assertEquals(WildState.Flee, state(WildRole.HATCHLING, playerDist = 100f))
    }

    @Test fun `a wounded predator breaks off and flees`() {
        assertEquals(WildState.Flee, state(WildRole.PREDATOR, hpFrac = 0.1f, preyNear = true, hunger = 1f))
    }
}
