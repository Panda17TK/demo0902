package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Pickup
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.systems.TraderRaidSystem
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.item.Trader
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.WorldMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.hypot

/** v2.110 宇宙の生きたイベント: the raid, the survivor, and the comet's dust-strung tail. */
class WorldSpaceEventsTest {
    private fun spaceWith(cond: (GameWorld) -> Boolean): GameWorld =
        (1L..60L).firstNotNullOf { s -> WorldFactory.create(InputState(), seed = s).takeIf(cond) }

    @Test fun `the raid descends at depth 4 and a defended vessel discounts its shelves`() {
        val gw = spaceWith { it.worldState.trader != null }
        assertEquals(0, gw.worldState.traderRaid, "quiet until the desync deepens")
        gw.waveState.num = TraderRaidSystem.RAID_WAVE
        gw.world.update(1f / 60f)
        assertEquals(1, gw.worldState.traderRaid, "the raid is on")
        assertTrue(gw.waveState.announce?.contains("襲われている") == true, "the band tells it")
        var raiders = 0
        with(gw.world) {
            gw.world.family { all(Mob) }.forEach { e -> if (e[Mob].raider) raiders++ }
        }
        assertEquals(TraderRaidSystem.RAIDERS, raiders)
        // put the raiders down — whoever does it, the vessel counts itself defended
        with(gw.world) {
            gw.world.family { all(Mob, Health) }.forEach { e -> if (e[Mob].raider) e[Health].hp = 0f }
        }
        repeat(3) { gw.world.update(1f / 60f) } // reap, then settle
        assertEquals(2, gw.worldState.traderRaid)
        assertTrue(gw.worldState.traderRescued, "the discount latch is set")
        assertEquals(80, Trader.discounted(100, true))
        assertEquals(100, Trader.discounted(100, false))
        assertEquals(1, Trader.discounted(1, true), "never below 1")
    }

    @Test fun `a sky without the vessel never raids`() {
        val gw = spaceWith { it.worldState.trader == null }
        gw.waveState.num = TraderRaidSystem.RAID_WAVE + 2
        repeat(3) { gw.world.update(1f / 60f) }
        assertEquals(0, gw.worldState.traderRaid)
    }

    @Test fun `some wreck fields shelter a survivor, and the index always points at a wreck`() {
        var with = 0; var without = 0
        for (s in 1L..40L) {
            val gw = WorldFactory.create(InputState(), seed = s)
            val idx = gw.worldState.survivorWreck
            if (idx >= 0) {
                with++
                assertTrue(idx < gw.worldState.wrecks.size, "seed $s: index $idx points at a real wreck")
            } else without++
        }
        assertTrue(with > 0 && without > 0, "the survivor visits some skies, not all ($with/$without)")
    }

    @Test fun `the comet strings dust beads down its tail, and surfaces carry none`() {
        val gw = spaceWith { it.worldState.comet != null }
        val (hx, hy) = gw.worldState.comet!!
        var beads = 0
        with(gw.world) {
            gw.world.family { all(Pickup, Transform) }.forEach { e ->
                val t = e[Transform]
                if (e[Pickup].kind == "dust" && hypot(t.x - hx, t.y - hy) < 600f) beads++
            }
        }
        assertTrue(beads >= 5, "a sweepable tail (got $beads beads)")
        val surf = WorldFactory.create(InputState(), seed = 3L, mode = WorldMode.SURFACE, biome = PlanetBiome.NATURE)
        assertNull(surf.worldState.comet)
        assertEquals(-1, surf.worldState.survivorWreck)
    }
}
