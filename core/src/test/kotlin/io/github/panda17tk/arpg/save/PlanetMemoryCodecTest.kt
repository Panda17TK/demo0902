package io.github.panda17tk.arpg.save

import io.github.panda17tk.arpg.sim.PlanetMemoryBook
import io.github.panda17tk.arpg.sim.PlanetSocietyState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlanetMemoryCodecTest {
    private fun dirtyBook(): PlanetMemoryBook {
        val book = PlanetMemoryBook()
        book.memories[42L] = PlanetSocietyState(
            childHarmed = true, childKilled = true, wildPredatorThreatenedChild = true,
            predatorKilledNearChild = true, hatchlingKilled = true, nestMotherKilled = true,
            apexKilled = true, surrenderKilled = 2, surrenderedSpared = 3,
            leaderDefeated = true, relicClaimed = true,
            hostility = 0.8f, mercy = 0.4f, ecologicalDisruption = 0.6f,
        )
        book.memories[7L] = PlanetSocietyState(mercy = 0.55f)
        return book
    }

    @Test fun `a book round-trips every field`() {
        val json = PlanetMemoryCodec.toJson(dirtyBook(), spaceSeed = 1L)
        val (seed, back) = PlanetMemoryCodec.fromJson(json)!!
        assertEquals(1L, seed)
        assertEquals(setOf(42L, 7L), back.memories.keys)
        val s = back.memories[42L]!!
        assertTrue(s.childKilled && s.apexKilled && s.leaderDefeated && s.relicClaimed)
        assertEquals(2, s.surrenderKilled)
        assertEquals(3, s.surrenderedSpared)
        assertEquals(0.8f, s.hostility)
        assertEquals(0.4f, s.mercy)
        assertEquals(0.6f, s.ecologicalDisruption)
        assertEquals(0.55f, back.memories[7L]!!.mercy)
    }

    @Test fun `unknown keys are ignored and missing fields default`() {
        val json = """{"version":1,"spaceSeed":1,"futureField":"?","planets":{"9":{"hostility":0.7,"someNewFlag":true}}}"""
        val (_, back) = PlanetMemoryCodec.fromJson(json)!!
        val s = back.memories[9L]!!
        assertEquals(0.7f, s.hostility)
        assertEquals(false, s.childKilled) // missing → default
        assertEquals(0f, s.mercy)
    }

    @Test fun `broken JSON reads as null`() {
        assertNull(PlanetMemoryCodec.fromJson("{corrupt"))
        assertNull(PlanetMemoryCodec.fromJson(""))
        assertNull(PlanetMemoryCodec.fromJson("[1,2,3]"))
    }
}
