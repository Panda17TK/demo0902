package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.config.EnemyDef
import io.github.panda17tk.arpg.config.FamilyRole
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.config.WildRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PredationTest {
    private fun wild(role: WildRole) =
        EnemyDef(name = role.name, hp = 50f, speed = 50f, lifeKind = LifeKind.WILDLIFE, wildRole = role)
    private fun child() =
        EnemyDef(name = "child", hp = 30f, speed = 50f, lifeKind = LifeKind.SAPIENT, familyRole = FamilyRole.CHILD)

    private val predator = wild(WildRole.PREDATOR)
    private val apex = wild(WildRole.APEX)
    private val prey = wild(WildRole.PREY)
    private val herd = wild(WildRole.HERD)
    private val nestGuard = wild(WildRole.NEST_GUARD)

    @Test fun `a predator preys on prey and herd`() {
        assertTrue(Predation.canPredate(predator, prey))
        assertTrue(Predation.canPredate(predator, herd))
    }

    @Test fun `a predator cannot prey on an apex`() {
        assertFalse(Predation.canPredate(predator, apex))
    }

    @Test fun `an apex preys on a lesser predator`() {
        assertTrue(Predation.canPredate(apex, predator))
    }

    @Test fun `a nest guard does not hunt`() {
        assertFalse(Predation.canPredate(nestGuard, prey))
    }

    @Test fun `a sapient child is huntable by hunters`() {
        assertTrue(Predation.canPredate(predator, child()))
        assertTrue(Predation.canPredate(apex, child()))
    }

    @Test fun `a non-wildlife creature never predates`() {
        val zombie = EnemyDef(name = "z", hp = 50f, speed = 50f) // default lifeKind = HOSTILE
        assertFalse(Predation.canPredate(zombie, prey))
    }

    @Test fun `bite damage is positive for hunters, harder for an apex, zero otherwise`() {
        assertTrue(Predation.biteDamage(predator) > 0f)
        assertTrue(Predation.biteDamage(apex) > Predation.biteDamage(predator))
        assertEquals(0f, Predation.biteDamage(nestGuard), 1e-6f)
    }

    @Test fun `feeding sates a hunter`() {
        assertTrue(Predation.feedingGain(predator, prey) > 0f)
        assertTrue(Predation.feedingGain(apex, prey) > 0f)
        assertEquals(0f, Predation.feedingGain(nestGuard, prey), 1e-6f)
    }
}
