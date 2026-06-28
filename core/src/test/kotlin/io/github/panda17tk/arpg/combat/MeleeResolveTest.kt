package io.github.panda17tk.arpg.combat

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MeleeResolveTest {
    @Test fun `full stamina swings a full sword`() {
        val o = MeleeResolve.resolve(staRatio = 1.0f)
        assertEquals(22f, o.dmg, 1e-3f); assertEquals(8f, o.slashDmg, 1e-3f); assertFalse(o.isFist)
    }
    @Test fun `low-ish stamina is a weakened sword`() {
        val o = MeleeResolve.resolve(staRatio = 0.30f)
        assertEquals(22f * 0.6f, o.dmg, 1e-3f); assertEquals(8f * 0.6f, o.slashDmg, 1e-3f); assertFalse(o.isFist)
    }
    @Test fun `very low stamina is a fist with no slash`() {
        val o = MeleeResolve.resolve(staRatio = 0.10f)
        assertEquals(8f, o.dmg, 1e-3f); assertEquals(0f, o.slashDmg, 1e-3f); assertTrue(o.isFist)
    }
}
