package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.45: the wave-flavor schedule is modular and learnable; names/rewards are deterministic. */
class WaveEventsTest {
    @Test fun `the schedule follows its modular rhythm`() {
        assertEquals(WaveEvent.BOUNTY, WaveEvents.eventFor(4))
        assertEquals(WaveEvent.BOUNTY, WaveEvents.eventFor(11))
        assertEquals(WaveEvent.BOUNTY, WaveEvents.eventFor(18))
        assertEquals(WaveEvent.HORDE, WaveEvents.eventFor(3))
        assertEquals(WaveEvent.HORDE, WaveEvents.eventFor(8))
        assertEquals(WaveEvent.HORDE, WaveEvents.eventFor(13))
        assertEquals(WaveEvent.STORM, WaveEvents.eventFor(5))
        assertEquals(WaveEvent.STORM, WaveEvents.eventFor(17))
        assertEquals(WaveEvent.STORM, WaveEvents.eventFor(29))
        assertEquals(WaveEvent.NONE, WaveEvents.eventFor(1))
        assertEquals(WaveEvent.NONE, WaveEvents.eventFor(2))
        assertEquals(WaveEvent.NONE, WaveEvents.eventFor(6))
        assertEquals(WaveEvent.NONE, WaveEvents.eventFor(7))
    }

    @Test fun `bounty beats horde beats storm on shared waves`() {
        // 23 ≡ 3 (mod 5) and ≡ 5 (mod 6): horde outranks storm.
        assertEquals(WaveEvent.HORDE, WaveEvents.eventFor(23))
        // 18 ≡ 4 (mod 7) and ≡ 3 (mod 5): bounty outranks horde.
        assertEquals(WaveEvent.BOUNTY, WaveEvents.eventFor(18))
    }

    @Test fun `bounty names are deterministic and two-part`() {
        assertEquals(WaveEvents.bountyName(Rng(7L)), WaveEvents.bountyName(Rng(7L)))
        assertTrue(WaveEvents.bountyName(Rng(1L)) != WaveEvents.bountyName(Rng(99L)) ||
            WaveEvents.bountyName(Rng(2L)) != WaveEvents.bountyName(Rng(98L)), "names vary by stream")
        assertTrue(WaveEvents.bountyName(Rng(3L)).length >= 3)
    }

    @Test fun `bounty reward grows with the wave`() {
        assertTrue(WaveEvents.bountyReward(20) > WaveEvents.bountyReward(4))
        assertTrue(WaveEvents.bountyReward(4) >= 40)
    }

    @Test fun `announcements exist for every event and only for events`() {
        assertNull(WaveEvents.announceFor(WaveEvent.NONE, null))
        assertTrue(WaveEvents.announceFor(WaveEvent.HORDE, null)!!.isNotEmpty())
        assertTrue(WaveEvents.announceFor(WaveEvent.STORM, null)!!.isNotEmpty())
        assertTrue(WaveEvents.announceFor(WaveEvent.BOUNTY, "宵のスズ")!!.contains("宵のスズ"))
    }
}
