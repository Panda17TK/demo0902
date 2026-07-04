package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlanetScanTest {
    private fun planet(id: Long = 42L, biome: PlanetBiome = PlanetBiome.NATURE, context: PlanetContext? = null) =
        PlanetBody(0f, 0f, 100f, 120f, 484f, biome, id, context ?: PlanetContext.contextFor(id, biome))

    @Test fun `the same planet always scans the same`() {
        val a = PlanetScan.cardFor(planet(), known = false, memory = PlanetSocietyState())
        val b = PlanetScan.cardFor(planet(), known = false, memory = PlanetSocietyState())
        assertEquals(a, b)
    }

    @Test fun `an unvisited planet reads 未訪問`() {
        val card = PlanetScan.cardFor(planet(), known = false, memory = PlanetSocietyState())
        assertEquals(PlanetScan.UNVISITED, card.memoryLine)
    }

    @Test fun `a hostile memory reuses ReturnVisitLine verbatim`() {
        val memory = PlanetSocietyState(hostility = 0.4f)
        val card = PlanetScan.cardFor(planet(), known = true, memory = memory)
        assertEquals(ReturnVisitLine.hudLine(memory), card.memoryLine)
        assertEquals("この星はあなたを敵として覚えている", card.memoryLine)
    }

    @Test fun `a faint memory reads 訪問済み　記憶は薄い`() {
        val memory = PlanetSocietyState(hostility = 0.2f, mercy = 0.2f)
        val card = PlanetScan.cardFor(planet(), known = true, memory = memory)
        assertEquals(PlanetScan.FAINT_MEMORY, card.memoryLine)
    }

    @Test fun `a planet without a story seed has no omen line`() {
        val ctx = PlanetContext(PlanetTemperament.GENTLE, SacredThing.CHILDREN, PlanetStorySeed.NONE)
        val card = PlanetScan.cardFor(planet(context = ctx), known = false, memory = PlanetSocietyState())
        assertNull(card.omenLine)
        assertEquals(3, card.lines.size) // trait + danger + memory (v2.43 adds the danger stars)
    }

    @Test fun `the card is composed from the lexicon and the biome name`() {
        val ctx = PlanetContext(PlanetTemperament.VENGEFUL, SacredThing.APEX, PlanetStorySeed.LOST_CHILD)
        val card = PlanetScan.cardFor(planet(biome = PlanetBiome.MAGMA, context = ctx), known = false, memory = PlanetSocietyState())
        assertEquals(PlanetBiome.MAGMA.displayName, card.title)
        assertTrue(card.traitLine.contains(PlanetLexicon.temperament(PlanetTemperament.VENGEFUL)))
        assertTrue(card.traitLine.contains(PlanetLexicon.sacred(SacredThing.APEX)))
        assertTrue(card.omenLine!!.contains(PlanetLexicon.omen(PlanetStorySeed.LOST_CHILD)!!))
    }
}
