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
        assertEquals(
            setOf(SystemTrait.STORMY, SystemTrait.HEAVY, SystemTrait.CUSTODIAL, SystemTrait.RICH),
            drawn.toSet(),
            "all four temperaments appear across seeds",
        )
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
