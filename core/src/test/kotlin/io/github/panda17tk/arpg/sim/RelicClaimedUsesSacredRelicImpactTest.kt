package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RelicClaimedUsesSacredRelicImpactTest {
    private fun ctx(s: SacredThing) = PlanetContext(PlanetTemperament.SILENT, s, PlanetStorySeed.NONE)

    @Test fun `carrying off the relic flags it and angers a relic-sacred world more`() {
        val sacred = PlanetSocietyState().also { it.onRelicClaimed(ctx(SacredThing.RELIC)) }
        val plain = PlanetSocietyState().also { it.onRelicClaimed(ctx(SacredThing.RUINS)) }
        assertTrue(sacred.relicClaimed && plain.relicClaimed, "the flag is set either way")
        assertTrue(sacred.hostility > plain.hostility, "${sacred.hostility} vs ${plain.hostility}")
    }
}
