package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.math.Rng
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LootTest {
    @Test fun `every wall drop includes blocks and only known kinds`() {
        val rng = Rng(1L)
        repeat(50) {
            val drops = Loot.wallDrops(rng)
            assertTrue(drops.isNotEmpty())
            assertTrue(drops.any { it.first == "blocks" }, "no blocks in $drops")
            assertTrue(drops.all { it.first in Loot.KINDS }, "unknown kind in $drops")
        }
    }

    @Test fun `wall drops can yield several items, not just one`() {
        val rng = Rng(7L)
        val maxN = (1..200).maxOf { Loot.wallDrops(rng).size }
        assertTrue(maxN >= 3, "max drop count was $maxN")
    }

    @Test fun `special items eventually appear among wall drops`() {
        val rng = Rng(3L)
        val kinds = (1..500).flatMap { Loot.wallDrops(rng) }.map { it.first }.toSet()
        assertTrue(kinds.any { it == "smoke" || it == "staminaInf" || it == "dashUp" }, "no special items in $kinds")
    }
}
