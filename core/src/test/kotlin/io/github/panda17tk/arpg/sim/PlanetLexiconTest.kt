package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlanetLexiconTest {
    @Test fun `every temperament has a word`() {
        for (t in PlanetTemperament.values()) {
            assertTrue(PlanetLexicon.temperament(t).isNotBlank(), "blank word for $t")
        }
    }

    @Test fun `every sacred thing has a word`() {
        for (s in SacredThing.values()) {
            assertTrue(PlanetLexicon.sacred(s).isNotBlank(), "blank word for $s")
        }
    }

    @Test fun `every story seed except NONE has an omen`() {
        for (seed in PlanetStorySeed.values()) {
            val omen = PlanetLexicon.omen(seed)
            if (seed == PlanetStorySeed.NONE) assertNull(omen, "NONE must carry no omen")
            else assertTrue(!omen.isNullOrBlank(), "blank omen for $seed")
        }
    }
}
