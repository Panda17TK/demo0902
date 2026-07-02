package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SocietyEventDiffTest {
    private val ctx = PlanetContext.NEUTRAL

    @Test fun `a rising flag emits exactly one event`() {
        val before = PlanetSocietyState()
        val after = PlanetSocietyState(relicClaimed = true)
        val events = SocietyEventDiff.diff(before, after, ctx)
        assertEquals(1, events.size)
        assertEquals(PlanetEventLines.line(SocietyDelta.RELIC_CLAIMED, ctx), events[0])
    }

    @Test fun `no change emits nothing`() {
        val s = PlanetSocietyState(childKilled = true, hostility = 0.8f)
        assertTrue(SocietyEventDiff.diff(s.copyState(), s, ctx).isEmpty())
    }

    @Test fun `several deeds in one tick come out in severity order`() {
        val before = PlanetSocietyState()
        val after = PlanetSocietyState(childKilled = true, childHarmed = true, apexKilled = true, relicClaimed = true)
        val texts = SocietyEventDiff.diff(before, after, ctx)
        assertEquals(
            listOf(
                PlanetEventLines.line(SocietyDelta.CHILD_KILLED, ctx),
                PlanetEventLines.line(SocietyDelta.APEX_KILLED, ctx),
                PlanetEventLines.line(SocietyDelta.RELIC_CLAIMED, ctx),
            ),
            texts,
        )
    }

    @Test fun `a killed child does not double-report the harm`() {
        val before = PlanetSocietyState()
        val after = PlanetSocietyState(childKilled = true, childHarmed = true)
        val events = SocietyEventDiff.diff(before, after, ctx)
        assertEquals(1, events.size)
        assertEquals(PlanetEventLines.line(SocietyDelta.CHILD_KILLED, ctx), events[0])
    }

    @Test fun `a harmed-but-alive child still reports the harm`() {
        val before = PlanetSocietyState()
        val after = PlanetSocietyState(childHarmed = true)
        assertEquals(listOf(PlanetEventLines.line(SocietyDelta.CHILD_HARMED, ctx)), SocietyEventDiff.diff(before, after, ctx))
    }

    @Test fun `hostility fires once when crossing the threshold and never again past it`() {
        val ctx = PlanetContext.NEUTRAL
        val crossing = SocietyEventDiff.diff(PlanetSocietyState(hostility = 0.55f), PlanetSocietyState(hostility = 0.65f), ctx)
        assertEquals(listOf(PlanetEventLines.line(SocietyDelta.HOSTILITY_CROSSED, ctx)), crossing)
        val past = SocietyEventDiff.diff(PlanetSocietyState(hostility = 0.65f), PlanetSocietyState(hostility = 0.9f), ctx)
        assertTrue(past.isEmpty())
    }

    @Test fun `mercy fires once when crossing its threshold`() {
        val crossing = SocietyEventDiff.diff(PlanetSocietyState(mercy = 0.4f), PlanetSocietyState(mercy = 0.5f), ctx)
        assertEquals(listOf(PlanetEventLines.line(SocietyDelta.MERCY_CROSSED, ctx)), crossing)
    }
}
