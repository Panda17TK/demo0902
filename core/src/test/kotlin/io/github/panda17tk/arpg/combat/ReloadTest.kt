package io.github.panda17tk.arpg.combat

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReloadTest {
    @Test fun `reload tops up the magazine from the reserve`() {
        val r = Reload.reload(magSize = 12, mag = 3, reserve = 100)
        assertEquals(12, r.newMag); assertEquals(91, r.newReserve); assertEquals(9, r.taken)
    }
    @Test fun `reload is limited by the reserve`() {
        val r = Reload.reload(magSize = 12, mag = 3, reserve = 4)
        assertEquals(7, r.newMag); assertEquals(0, r.newReserve); assertEquals(4, r.taken)
    }
    @Test fun `full magazine takes nothing`() {
        val r = Reload.reload(magSize = 12, mag = 12, reserve = 100)
        assertEquals(12, r.newMag); assertEquals(100, r.newReserve); assertEquals(0, r.taken)
    }
}
