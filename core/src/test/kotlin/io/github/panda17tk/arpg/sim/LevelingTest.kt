package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LevelingTest {
    @Test fun `a higher-level killer earns more xp per kill`() {
        assertTrue(Leveling.xpForKill(3) > Leveling.xpForKill(1))
    }

    @Test fun `the level-up threshold grows with level`() {
        assertTrue(Leveling.threshold(3) > Leveling.threshold(1))
    }

    @Test fun `attack variety rises with level but is capped by the archetype`() {
        assertEquals(1, Leveling.attacksForLevel(1, 3))
        assertEquals(2, Leveling.attacksForLevel(2, 3))
        assertEquals(3, Leveling.attacksForLevel(9, 3)) // capped at max
        assertEquals(0, Leveling.attacksForLevel(9, 0)) // no attacks defined → none
    }

    @Test fun `smarts rises with both level and tribe intelligence`() {
        assertTrue(Leveling.smarts(0.5f, 3) > Leveling.smarts(0.5f, 1))
        assertTrue(Leveling.smarts(0.8f, 1) > Leveling.smarts(0.2f, 1))
    }
}
