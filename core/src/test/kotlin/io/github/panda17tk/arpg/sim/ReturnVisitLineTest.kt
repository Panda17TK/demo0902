package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReturnVisitLineTest {
    private val hostile = PlanetSocietyState(hostility = 0.5f)
    private val hostileChild = PlanetSocietyState(hostility = 0.5f, childHarmed = true)
    private val merciful = PlanetSocietyState(mercy = 0.5f)
    private val guardian = PlanetSocietyState(mercy = 0.5f, predatorKilledNearChild = true)

    @Test fun `salt zero keeps the legacy sentences`() {
        assertEquals("この星はあなたを敵として覚えている", ReturnVisitLine.hudLine(hostile))
        assertEquals("森は前の傷を覚えている", ReturnVisitLine.hudLine(hostileChild))
        assertEquals("この星はあなたへの借りを覚えている", ReturnVisitLine.hudLine(merciful))
        assertEquals("守護者はまだ借りを覚えている", ReturnVisitLine.hudLine(guardian))
    }

    @Test fun `different salts vary the voice within the same deed`() {
        val voices = (0..2).map { ReturnVisitLine.hudLine(hostile, it)!! }.toSet()
        assertTrue(voices.size > 1, "expected variation, got $voices")
    }

    @Test fun `every variation is non-blank and deterministic`() {
        for (s in listOf(hostile, hostileChild, merciful, guardian)) {
            for (salt in 0..5) {
                val line = ReturnVisitLine.hudLine(s, salt)!!
                assertTrue(line.isNotBlank())
                assertEquals(line, ReturnVisitLine.hudLine(s, salt))
            }
        }
    }

    @Test fun `a faint memory still has no line at any salt`() {
        for (salt in 0..5) assertNull(ReturnVisitLine.hudLine(PlanetSocietyState(hostility = 0.2f), salt))
    }

    @Test fun `memory tones follow the dominant feeling`() {
        assertEquals(0, ReturnVisitEffects.memoryTone(PlanetSocietyState()))
        assertEquals(1, ReturnVisitEffects.memoryTone(PlanetSocietyState(hostility = 0.7f)))
        assertEquals(2, ReturnVisitEffects.memoryTone(PlanetSocietyState(mercy = 0.6f)))
        assertEquals(1, ReturnVisitEffects.memoryTone(PlanetSocietyState(hostility = 0.9f, mercy = 0.6f)))
    }
}
