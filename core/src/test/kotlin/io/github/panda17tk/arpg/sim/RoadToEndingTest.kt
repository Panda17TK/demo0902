package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.155 結末への道: the reveal lands at system 3, and the core visit re-arms by leaving. */
class RoadToEndingTest {
    @Test fun `system three hears the reveal`() {
        for (id in 1L..12L) {
            val line = MemoryCoreLog.lineFor(id, PlanetBiome.NATURE, system = 3)
            assertTrue(MemoryCoreLog.REVEAL.any { line.endsWith(it) }, "system 3 speaks the reveal: $line")
        }
        // system 2 still keeps the AWARE band — the staging survives
        val early = MemoryCoreLog.lineFor(5L, PlanetBiome.NATURE, system = 2)
        assertTrue(MemoryCoreLog.REVEAL.none { early.endsWith(it) }, "system 2 stays before the reveal")
    }

    @Test fun `the core visit arms fresh and disarms on approach`() {
        val ws = WorldState()
        assertTrue(ws.coreArmed, "a fresh surface starts armed")
        ws.snapshotQuestBases()
        assertTrue(!ws.coreVisited, "the stage seam lowers the visit flag")
    }
}
