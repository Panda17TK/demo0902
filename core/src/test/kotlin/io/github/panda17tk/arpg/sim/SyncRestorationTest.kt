package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.52 星間同期復旧度: jumps + visits + trust knit the network back — never quite to 100. */
class SyncRestorationTest {
    @Test fun `a fresh drifter has restored nothing`() {
        assertEquals(0, SyncRestoration.percent(1, emptyList()))
    }

    @Test fun `jumps, visits and trust all raise the percentage`() {
        val plain = PlanetSocietyState()
        val trusted = PlanetSocietyState(mercy = 0.7f)
        val base = SyncRestoration.percent(1, listOf(plain))
        assertTrue(SyncRestoration.percent(2, listOf(plain)) > base, "a jump helps")
        assertTrue(SyncRestoration.percent(1, listOf(plain, plain)) > base, "a visit helps")
        assertTrue(SyncRestoration.percent(1, listOf(trusted)) > base, "trust helps most per planet")
    }

    @Test fun `restoration caps at 99 — the last percent is gone with the readers`() {
        val trusted = PlanetSocietyState(mercy = 0.9f)
        assertEquals(99, SyncRestoration.percent(50, List(50) { trusted }))
    }

    @Test fun `a well-restored network asks one shard less at the gate`() {
        assertEquals(3, SyncRestoration.gateShardsNeeded(0))
        assertEquals(3, SyncRestoration.gateShardsNeeded(59))
        assertEquals(2, SyncRestoration.gateShardsNeeded(60))
        assertEquals(2, SyncRestoration.gateShardsNeeded(99))
    }
}
