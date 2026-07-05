package io.github.panda17tk.arpg.planet

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.45/68/69 星の依頼: one deterministic request per planet, with sane targets and pay. */
class PlanetQuestTest {
    @Test fun `a planet's request is deterministic`() {
        assertEquals(PlanetQuest.questFor(42L, PlanetBiome.ICE), PlanetQuest.questFor(42L, PlanetBiome.ICE))
    }

    @Test fun `targets and rewards stay in range for many planets`() {
        for (id in 0L..60L) for (b in PlanetBiome.entries) {
            val q = PlanetQuest.questFor(id, b)
            when (q.kind) {
                QuestKind.ELITES -> assertTrue(q.target in 1..3, "elites 1..3, got ${q.target}")
                QuestKind.KILLS -> assertTrue(q.target in 8..16, "kills 8..16, got ${q.target}")
                QuestKind.DUST -> assertTrue(q.target in 20..40, "dust 20..40, got ${q.target}")
                QuestKind.CORE -> assertEquals(1, q.target, "the core asks exactly one visit")
                QuestKind.PROTECT -> assertTrue(q.target in 2..3, "predators 2..3, got ${q.target}")
                QuestKind.OBSERVE -> assertTrue(q.target in 45..90, "observe 45..90s, got ${q.target}")
            }
            assertTrue(q.rewardDust > 0)
            assertTrue(q.line.contains("依頼"))
        }
    }

    @Test fun `requests vary across planets`() {
        val quests = (0L..30L).map { PlanetQuest.questFor(it, PlanetBiome.NATURE) }.toSet()
        assertTrue(quests.size > 3, "expected variety, got ${quests.size} distinct quests")
    }

    @Test fun `every request kind actually appears in the pool`() {
        // v2.68/69: the quiet asks (DUST/CORE/PROTECT/OBSERVE) must be reachable, not dead entries.
        val seen = mutableSetOf<QuestKind>()
        for (id in 0L..300L) for (b in PlanetBiome.entries) seen.add(PlanetQuest.questFor(id, b).kind)
        assertEquals(QuestKind.entries.toSet(), seen)
    }

    @Test fun `the chain keeps stage 0 historic and varies the later links`() {
        // v2.72: what a star asks FIRST is byte-identical to the pre-chain quest...
        assertEquals(PlanetQuest.questFor(7L, PlanetBiome.NATURE), PlanetQuest.questFor(7L, PlanetBiome.NATURE, 0))
        assertEquals(3, PlanetQuest.CHAIN)
        // ...and the later links are their own rolls, not echoes of the first.
        var varied = 0
        for (id in 0L..40L) {
            if (PlanetQuest.questFor(id, PlanetBiome.NATURE, 0) != PlanetQuest.questFor(id, PlanetBiome.NATURE, 1)) varied++
        }
        assertTrue(varied > 20, "stages barely vary ($varied/41)")
    }

    @Test fun `the lonely asteroid never asks for an escort`() {
        // v2.69: LONELY has no wildlife — a PROTECT request there could never be fulfilled.
        for (id in 0L..300L) for (stage in 0 until PlanetQuest.CHAIN) {
            val q = PlanetQuest.questFor(id, PlanetBiome.LONELY, stage)
            assertTrue(q.kind != QuestKind.PROTECT, "planet $id stage $stage asked LONELY for protection")
        }
    }
}
