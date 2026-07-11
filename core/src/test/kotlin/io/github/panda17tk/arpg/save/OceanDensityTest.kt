package io.github.panda17tk.arpg.save

import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.world.WorldFactory
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.165 海の密度: a thinner ocean is a strict subset of the full one — nothing moves, some stay home. */
class OceanDensityTest {
    @Test fun `the tiers map to strides and the tap cycles downward`() {
        assertEquals(1, OceanDensity.keep(OceanDensity.HIGH))
        assertEquals(3, OceanDensity.keep(OceanDensity.MEDIUM))
        assertEquals(7, OceanDensity.keep(OceanDensity.LOW))
        assertEquals(OceanDensity.MEDIUM, OceanDensity.next(OceanDensity.HIGH))
        assertEquals(OceanDensity.LOW, OceanDensity.next(OceanDensity.MEDIUM))
        assertEquals(OceanDensity.HIGH, OceanDensity.next(OceanDensity.LOW))
        assertEquals(OceanDensity.MEDIUM, OceanDensity.tier, "the shipped default is 中")
    }

    @Test fun `a medium ocean is a strict subset of the high ocean and keeps its landmarks`() {
        fun ocean(keep: Int): Pair<Set<Pair<Float, Float>>, Map<String, Int>> {
            val gw = WorldFactory.create(InputState(), seed = 3L, oceanKeep = keep)
            val pos = HashSet<Pair<Float, Float>>()
            val kinds = HashMap<String, Int>()
            with(gw.world) {
                gw.world.family { all(Mob, Transform) }.forEach { e ->
                    val m = e[Mob]
                    if (m.def.lifeKind != LifeKind.WILDLIFE) return@forEach
                    val t = e[Transform]
                    pos.add(t.x to t.y)
                    kinds.merge(m.def.id, 1, Int::plus)
                }
            }
            return pos to kinds
        }
        val (hiPos, hiKinds) = ocean(1)
        val (midPos, midKinds) = ocean(3)
        assertTrue(hiPos.containsAll(midPos), "every kept fish swims exactly where the full ocean put it")
        assertTrue(midPos.size in hiPos.size / 6..hiPos.size * 45 / 100,
            "中 runs near a third of 高 (${midPos.size} of ${hiPos.size})")
        // the landmarks of the sky (count-1 spawns ride index 0) survive every tier
        assertEquals(hiKinds["isle_whale"], midKinds["isle_whale"], "the whales remain")
        assertEquals(hiKinds["tyrant_shark"], midKinds["tyrant_shark"], "the tyrant remains")
        val (loPos, _) = ocean(7)
        // keep=7 keeps indices 0,7,14,… — multiples of 7, not of 3 — so 低 nests in 高, not in 中
        assertTrue(hiPos.containsAll(loPos), "低 nests inside 高 the same way")
        assertTrue(loPos.size < midPos.size, "and it is thinner still (${loPos.size} vs ${midPos.size})")
    }
}
