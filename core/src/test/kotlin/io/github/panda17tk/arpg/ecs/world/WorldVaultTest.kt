package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Pickup
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.map.SurfaceVault
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.sim.WorldMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.hypot

/** v2.95 地下遺構: the sealed chamber, its one mouth, its keeper and its hoard. */
class WorldVaultTest {
    private fun surface(seed: Long) = WorldFactory.create(
        InputState(), seed = seed, mode = WorldMode.SURFACE, biome = PlanetBiome.NATURE,
    )

    @Test fun `every surface hides one sealed vault a real walk from the pad`() {
        val gw = surface(42L)
        val vault = gw.worldState.vault
        assertTrue(vault != null, "the vault exists")
        val (vx, vy) = vault!!
        val pad = gw.worldState.escapePad!!
        assertTrue(hypot(vx - pad.first, vy - pad.second) > 30 * Tuning.TILE, "a real walk from the pad")
    }

    @Test fun `the plating is indestructible with a single two-tile mouth`() {
        val gw = surface(42L)
        val (vx, vy) = gw.worldState.vault!!
        val cx = (vx / Tuning.TILE).toInt(); val cy = (vy / Tuning.TILE).toInt()
        var solid = 0; var open = 0
        val r = SurfaceVault.R
        for (dy in -r..r) for (dx in -r..r) {
            if (maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) == r) {
                if (gw.map.solidAt(cx + dx, cy + dy)) solid++ else open++
            } else {
                assertTrue(!gw.map.solidAt(cx + dx, cy + dy), "the chamber floor stays open")
            }
        }
        assertEquals(2, open, "one two-tile mouth")
        assertEquals(8 * r - 2, solid, "the rest of the ring is plated")
    }

    @Test fun `a keeper waits inside with the hoard`() {
        val gw = surface(42L)
        val (vx, vy) = gw.worldState.vault!!
        var keeper = false
        with(gw.world) {
            gw.world.family { all(Mob, Transform) }.forEach { e ->
                val t = e[Transform]
                if (hypot(t.x - vx, t.y - vy) < Tuning.TILE * 3f && e[Mob].tier == "midboss") {
                    keeper = true
                    assertEquals(6, e[Mob].level, "the keeper outranks the field")
                }
            }
        }
        assertTrue(keeper, "the vault has its keeper")
        var loot = 0
        with(gw.world) {
            gw.world.family { all(Pickup, Transform) }.forEach { e ->
                val t = e[Transform]
                if (hypot(t.x - vx, t.y - vy) < Tuning.TILE * 2f) loot++
            }
        }
        assertTrue(loot >= 3, "dust, a med case and the biome relic wait inside (got $loot)")
    }

    @Test fun `space carries no vault`() {
        val gw = WorldFactory.create(InputState(), seed = 42L)
        assertNull(gw.worldState.vault)
    }
}
