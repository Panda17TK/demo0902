package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SocietyMemorySummaryTest {
    @Test fun `an untouched visit shows only the relic hint`() {
        val facts = SocietyMemorySummary.factLines(PlanetSocietyState())
        assertEquals(1, facts.size)
        assertEquals(Mark.NONE, facts[0].second)
        assertTrue(facts[0].first.contains("遺物"))
    }

    @Test fun `a killed child leads the list`() {
        val s = PlanetSocietyState(childKilled = true, leaderDefeated = true, relicClaimed = true)
        val facts = SocietyMemorySummary.factLines(s)
        assertEquals(Mark.BAD, facts[0].second)
        assertTrue(facts[0].first.contains("子"))
    }

    @Test fun `a kill is not double-listed as a harm`() {
        val s = PlanetSocietyState(childKilled = true, childHarmed = true)
        val facts = SocietyMemorySummary.factLines(s)
        assertEquals(1, facts.count { it.first.contains("子を") })
    }

    @Test fun `the list never exceeds its cap`() {
        val s = PlanetSocietyState(
            childKilled = true, childHarmed = true, apexKilled = true, nestMotherKilled = true,
            hatchlingKilled = true, predatorKilledNearChild = true, leaderDefeated = true, relicClaimed = true,
        )
        assertTrue(SocietyMemorySummary.factLines(s).size <= SocietyMemorySummary.MAX_FACTS)
    }

    @Test fun `gauges expose the three feelings in order`() {
        val s = PlanetSocietyState(hostility = 0.7f, mercy = 0.3f, ecologicalDisruption = 0.5f)
        val gauges = SocietyMemorySummary.gauges(s)
        assertEquals(listOf("敵意" to 0.7f, "感謝" to 0.3f, "乱れ" to 0.5f), gauges)
    }

    @Test fun `reading the summary never mutates the society`() {
        val s = PlanetSocietyState(childKilled = true, hostility = 0.8f)
        val before = s.copyState()
        SocietyMemorySummary.factLines(s)
        SocietyMemorySummary.gauges(s)
        assertEquals(before.childKilled, s.childKilled)
        assertEquals(before.hostility, s.hostility)
        assertEquals(before.relicClaimed, s.relicClaimed)
    }
}
