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

    @Test fun `the plating is indestructible with its form's mouths`() {
        val gw = surface(42L)
        val (vx, vy) = gw.worldState.vault!!
        val cx = (vx / Tuning.TILE).toInt(); val cy = (vy / Tuning.TILE).toInt()
        val form = SurfaceVault.formFor(42L) // v2.109: the vault takes one of three forms
        val r = SurfaceVault.radiusFor(form)
        var solid = 0; var open = 0
        for (dy in -r..r) for (dx in -r..r) {
            if (maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) == r) {
                if (gw.map.solidAt(cx + dx, cy + dy)) solid++ else open++
            }
        }
        assertEquals(SurfaceVault.mouthTiles(form), open, "form $form carries its own mouths")
        assertEquals(8 * r - SurfaceVault.mouthTiles(form), solid, "the rest of the ring is plated")
        assertTrue(!gw.map.solidAt(cx, cy), "the heart tile stays open in every form")
    }

    // ── v2.109 深掘り: the three forms ──

    private fun seedWithForm(form: Int): Long =
        (1L..120L).first { SurfaceVault.formFor(it) == form }

    @Test fun `all three forms appear across seeds`() {
        val seen = (1L..120L).map { SurfaceVault.formFor(it) }.toSet()
        assertEquals(setOf(0, 1, 2), seen)
    }

    @Test fun `二重輪 walls its heart behind an inner ring`() {
        val seed = seedWithForm(1)
        val gw = surface(seed)
        val (vx, vy) = gw.worldState.vault!!
        val cx = (vx / Tuning.TILE).toInt(); val cy = (vy / Tuning.TILE).toInt()
        var innerSolid = 0
        for (dy in -1..1) for (dx in -1..1) {
            if (maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) == 1 && gw.map.solidAt(cx + dx, cy + dy)) innerSolid++
        }
        assertEquals(7, innerSolid, "the inner ring is plated but for its one-tile mouth")
    }

    @Test fun `大房 posts two watchers over a fatter hoard`() {
        val seed = seedWithForm(2)
        val gw = surface(seed)
        val (vx, vy) = gw.worldState.vault!!
        var guards = 0; var dust = 0; var guns = 0
        with(gw.world) {
            gw.world.family { all(Mob, Transform) }.forEach { e ->
                val t = e[Transform]
                if (hypot(t.x - vx, t.y - vy) < Tuning.TILE * 3f && e[Mob].tier == "midboss") guards++
            }
            gw.world.family { all(Pickup, Transform) }.forEach { e ->
                val t = e[Transform]
                if (hypot(t.x - vx, t.y - vy) < Tuning.TILE * 2f) {
                    if (e[Pickup].kind == "dust") dust += e[Pickup].amount
                    if (e[Pickup].kind.startsWith("item:")) guns++
                }
            }
        }
        assertEquals(2, guards, "two watchers for two mouths")
        assertTrue(dust >= 140, "the great hall pays better (got $dust)")
        assertEquals(1, guns, "a weapon cache waits inside")
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
