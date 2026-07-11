package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.ecs.systems.DesyncSurgeSystem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.156 変化する波: the early pool changes face per system, and all three bosses stalk the void. */
class WaveVarietyTest {
    @Test fun `the stratified shuffle keeps bands and determinism`() {
        val keys = (1..20).map { "k$it" }
        val a = DesyncSurgeSystem.stratifiedShuffle(keys, 7L)
        val b = DesyncSurgeSystem.stratifiedShuffle(keys, 7L)
        val c = DesyncSurgeSystem.stratifiedShuffle(keys, 8L)
        assertEquals(a, b, "same seed shuffles the same")
        assertTrue(a != c || keys.size < 6, "a different sky deals a different hand")
        assertEquals(keys.toSet(), a.toSet(), "a permutation loses no one")
        // stratigraphy: the first band of the output is drawn only from the first band of the input
        assertTrue(a.take(6).all { it in keys.take(6) }, "shallow layers stay shallow")
        assertTrue(a.drop(18).all { it in keys.drop(18) }, "the machinery stays deep")
    }

    @Test fun `all three bosses stalk the void`() {
        val bosses = GameConfig().enemies.filterValues { it.tier == "boss" && it.lifeKind != LifeKind.WILDLIFE }
        assertEquals(setOf("overlord", "storm_core", "exiled_king"), bosses.keys,
            "the boss pool holds all three faces — Overlord alone was every tenth wave")
    }
}
