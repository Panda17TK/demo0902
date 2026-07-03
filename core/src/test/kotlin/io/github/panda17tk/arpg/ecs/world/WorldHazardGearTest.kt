package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Buff
import io.github.panda17tk.arpg.ecs.components.Gear
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.item.EquipSlot
import io.github.panda17tk.arpg.item.ItemCatalog
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.WorldMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.35 special effects against the terrain hazards: heat-proof gear vs the magma burn, regen. */
class WorldHazardGearTest {
    private fun magmaWorld() = WorldFactory.create(
        InputState(), seed = 7L, mode = WorldMode.SURFACE, biome = PlanetBiome.MAGMA,
    )

    private fun hp(gw: GameWorld): Float = with(gw.world) { gw.player[Health].hp }

    @Test fun `a magma surface burns an unprotected player`() {
        val gw = magmaWorld()
        val before = hp(gw)
        repeat(30) { gw.world.update(1f / 60f) } // half a second on the burning surface
        assertTrue(hp(gw) < before, "expected the magma surface to burn HP")
    }

    @Test fun `the thermal suit cuts the magma burn completely`() {
        val gw = magmaWorld()
        with(gw.world) { gw.player[Gear].loadout.set(EquipSlot.ARMOR, ItemCatalog.byId("armor_thermal")!!) }
        val before = hp(gw)
        repeat(30) { gw.world.update(1f / 60f) }
        assertEquals(before, hp(gw), 1e-3f)
    }

    @Test fun `the heat coating protects for its duration`() {
        val gw = magmaWorld()
        with(gw.world) { gw.player[Buff].heatProofT = 60f }
        val before = hp(gw)
        repeat(30) { gw.world.update(1f / 60f) }
        assertEquals(before, hp(gw), 1e-3f)
    }

    @Test fun `repair patches regenerate lost HP`() {
        val gw = WorldFactory.create(InputState(), seed = 7L) // space: no hazards, no hostile spawn nearby
        with(gw.world) {
            gw.player[Gear].loadout.set(EquipSlot.ACC1, ItemCatalog.byId("acc_repair")!!)
            gw.player[Health].hp = 50f
        }
        repeat(60) { gw.world.update(1f / 60f) } // one second → +1.5 HP
        val after = hp(gw)
        assertTrue(after > 50.5f && after <= 52.5f, "expected ~1.5 HP regenerated, got ${after - 50f}")
    }
}
