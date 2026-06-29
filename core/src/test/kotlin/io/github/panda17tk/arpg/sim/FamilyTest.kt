package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.config.FamilyRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FamilyTest {
    @Test fun `children elders and kings are wards`() {
        assertTrue(Family.isWard(FamilyRole.CHILD))
        assertTrue(Family.isWard(FamilyRole.ELDER))
        assertTrue(Family.isWard(FamilyRole.KING))
    }

    @Test fun `guardians and commoners are not wards`() {
        assertFalse(Family.isWard(FamilyRole.GUARDIAN))
        assertFalse(Family.isWard(FamilyRole.NONE))
    }

    @Test fun `a nearby king raises bravery`() {
        assertTrue(Family.effectiveBravery(0.2f, kingNear = true) > 0.2f)
    }

    @Test fun `bravery is unchanged without a king and capped at 1`() {
        assertEquals(0.2f, Family.effectiveBravery(0.2f, false), 1e-6f)
        assertEquals(1f, Family.effectiveBravery(0.9f, true), 1e-6f)
    }
}
