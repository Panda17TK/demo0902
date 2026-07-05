package io.github.panda17tk.arpg.planet

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.45/68 星の依頼: one deterministic request per planet, with sane targets and pay. */
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
        // v2.68: the two quiet asks (DUST / CORE) must be reachable, not dead enum entries.
        val seen = mutableSetOf<QuestKind>()
        for (id in 0L..300L) for (b in PlanetBiome.entries) seen.add(PlanetQuest.questFor(id, b).kind)
        assertEquals(QuestKind.entries.toSet(), seen)
    }
}
