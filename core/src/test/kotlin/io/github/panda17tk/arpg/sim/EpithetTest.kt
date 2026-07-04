package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.50 呼び名 + 同期汚染ゲージ: pure reputation title + stability decay. */
class EpithetTest {
    @Test fun `a stranger starts as 異邦人`() {
        assertEquals("異邦人", Epithet.of(emptyList()))
        assertEquals("異邦人", Epithet.of(listOf(PlanetSocietyState())))
    }

    @Test fun `a child's death outranks everything`() {
        val kind = PlanetSocietyState(mercy = 0.9f)
        val killer = PlanetSocietyState(childKilled = true)
        assertEquals("星喰い", Epithet.of(listOf(kind, kind, killer)))
    }

    @Test fun `repeated deeds earn their names`() {
        val kingslayer = PlanetSocietyState(leaderDefeated = true)
        assertEquals("王殺し", Epithet.of(listOf(kingslayer, kingslayer)))
        val taker = PlanetSocietyState(relicClaimed = true)
        assertEquals("遺物持ち", Epithet.of(listOf(taker, taker)))
        val giver = PlanetSocietyState(mercy = 0.6f)
        assertEquals("星還し", Epithet.of(listOf(giver, giver)))
    }

    @Test fun `just travelling makes a 巡回者`() {
        val plain = PlanetSocietyState()
        assertEquals("巡回者", Epithet.of(listOf(plain, plain, plain)))
    }

    @Test fun `stability decays with the surge level but never dies`() {
        assertEquals(100, DesyncGauge.stability(1))
        assertTrue(DesyncGauge.stability(10) < DesyncGauge.stability(2))
        assertEquals(5, DesyncGauge.stability(999))
    }
}
