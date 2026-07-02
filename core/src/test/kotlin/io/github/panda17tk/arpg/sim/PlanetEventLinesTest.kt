package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlanetEventLinesTest {
    @Test fun `every delta has a non-empty sentence`() {
        for (d in SocietyDelta.values()) {
            val e = PlanetEventLines.line(d, PlanetContext.NEUTRAL)
            assertTrue(e.text.isNotBlank(), "blank line for $d")
        }
    }

    @Test fun `a deed against the sacred reads one register harsher`() {
        val childrenSacred = PlanetContext(PlanetTemperament.GENTLE, SacredThing.CHILDREN, PlanetStorySeed.NONE)
        assertNotEquals(
            PlanetEventLines.line(SocietyDelta.CHILD_KILLED, PlanetContext.NEUTRAL).text,
            PlanetEventLines.line(SocietyDelta.CHILD_KILLED, childrenSacred).text,
        )
        val apexSacred = PlanetContext(PlanetTemperament.GENTLE, SacredThing.APEX, PlanetStorySeed.NONE)
        assertNotEquals(
            PlanetEventLines.line(SocietyDelta.APEX_KILLED, PlanetContext.NEUTRAL).text,
            PlanetEventLines.line(SocietyDelta.APEX_KILLED, apexSacred).text,
        )
    }

    @Test fun `kinds map to the intended colour families`() {
        assertEquals(EventKind.HOSTILE, PlanetEventLines.line(SocietyDelta.CHILD_KILLED, PlanetContext.NEUTRAL).kind)
        assertEquals(EventKind.MERCY, PlanetEventLines.line(SocietyDelta.PREDATOR_REPELLED, PlanetContext.NEUTRAL).kind)
        assertEquals(EventKind.ECOLOGY, PlanetEventLines.line(SocietyDelta.APEX_KILLED, PlanetContext.NEUTRAL).kind)
        assertEquals(EventKind.NEUTRAL, PlanetEventLines.line(SocietyDelta.LEADER_DEFEATED, PlanetContext.NEUTRAL).kind)
    }
}
