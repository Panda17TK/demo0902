package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.config.WorkshopBoons
import io.github.panda17tk.arpg.ecs.world.WorldFactory
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.91 星系の個性: deterministic temperaments and the rules they bend. */
class SystemTraitsTest {
    @Test fun `the first system is always the calm teaching sky`() {
        assertEquals(SystemTrait.NONE, SystemTraits.traitFor(1L))
        assertEquals(SystemTrait.NONE, SystemTraits.traitFor(0L))
    }

    @Test fun `later systems draw deterministically and every temperament exists`() {
        val drawn = (2L..40L).map { SystemTraits.traitFor(it) }
        assertEquals(drawn, (2L..40L).map { SystemTraits.traitFor(it) }, "same seed, same sky")
        assertTrue(drawn.none { it == SystemTrait.NONE }, "past the first, every system has a temperament")
        val all = setOf(
            SystemTrait.STORMY, SystemTrait.HEAVY, SystemTrait.CUSTODIAL, SystemTrait.RICH, SystemTrait.SWARMING,
            SystemTrait.DESOLATE, SystemTrait.RESONANT, SystemTrait.AIRY, // v2.183 濃い外縁II
        )
        assertTrue(drawn.toSet().all { it in all }, "every draw is a known temperament")
        assertTrue(
            drawn.any { it in setOf(SystemTrait.DESOLATE, SystemTrait.RESONANT, SystemTrait.AIRY) },
            "the rarer rim temperaments surface across seeds", // v2.183
        )
        assertTrue(drawn.toSet().size >= 5, "the sky stays varied")
    }

    @Test fun `quiet waves inherit the weather but scheduled events always win`() {
        assertEquals(WaveEvent.STORM, SystemTraits.fillEvent(SystemTrait.STORMY, WaveEvent.NONE, 2))
        assertEquals(WaveEvent.BOUNTY, SystemTraits.fillEvent(SystemTrait.STORMY, WaveEvent.BOUNTY, 4))
        assertEquals(WaveEvent.PURGE, SystemTraits.fillEvent(SystemTrait.CUSTODIAL, WaveEvent.NONE, 4))
        assertEquals(WaveEvent.NONE, SystemTraits.fillEvent(SystemTrait.CUSTODIAL, WaveEvent.NONE, 2))
        assertEquals(WaveEvent.NONE, SystemTraits.fillEvent(SystemTrait.NONE, WaveEvent.NONE, 2))
    }

    @Test fun `rich systems pay more and press harder — heavy ones just pull`() {
        assertEquals(1, SystemTraits.dustBonus(SystemTrait.RICH))
        assertEquals(0, SystemTraits.dustBonus(SystemTrait.STORMY))
        assertTrue(SystemTraits.quotaMul(SystemTrait.RICH) > 1f)
        assertEquals(1.5f, SystemTraits.gravityMul(SystemTrait.HEAVY))
        assertEquals(1f, SystemTraits.gravityMul(SystemTrait.RICH))
        // v2.183 濃い外縁II: the three rim temperaments
        assertEquals(1, SystemTraits.dustBonus(SystemTrait.DESOLATE), "lean but precious")
        assertTrue(SystemTraits.quotaMul(SystemTrait.DESOLATE) < 1f, "a starved sky asks for fewer")
        assertEquals(0.6f, SystemTraits.gravityMul(SystemTrait.AIRY), "an airy sky barely pulls")
        assertEquals(WaveEvent.STORM, SystemTraits.fillEvent(SystemTrait.RESONANT, WaveEvent.NONE, 2), "the rim rings on even waves")
        assertEquals(WaveEvent.NONE, SystemTraits.fillEvent(SystemTrait.RESONANT, WaveEvent.NONE, 3), "…and rests on odd")
    }

    @Test fun `the trait reaches the world state in space and never on a surface`() {
        val space = WorldFactory.create(InputState(), seed = 2L, boons = WorkshopBoons.NONE, trait = SystemTrait.HEAVY)
        assertEquals(SystemTrait.HEAVY, space.worldState.trait)
        val surface = WorldFactory.create(
            InputState(), seed = 2L, mode = WorldMode.SURFACE,
            biome = io.github.panda17tk.arpg.planet.PlanetBiome.NATURE,
            trait = SystemTrait.HEAVY,
        )
        assertEquals(SystemTrait.NONE, surface.worldState.trait, "a surface walk ignores the sky's temperament")
    }
}
