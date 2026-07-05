package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.combat.Weapons
import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.Bullet
import io.github.panda17tk.arpg.ecs.components.Cooldowns
import io.github.panda17tk.arpg.ecs.components.Gear
import io.github.panda17tk.arpg.ecs.components.Grenade
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.item.EquipSlot
import io.github.panda17tk.arpg.item.ItemCatalog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.37 weapon grades: per-gun handling riding into the fired projectiles + infinite mg/beam. */
class WorldWeaponGradeTest {
    private fun world(input: InputState) = WorldFactory.create(input, seed = 3L)

    /** Equip [gunId] (swapping whatever is in RANGED into the backpack) and point Arsenal at its type. */
    private fun equipGun(gw: GameWorld, gunId: String) = with(gw.world) {
        val gear = gw.player[Gear]
        val item = ItemCatalog.byId(gunId)!!
        val prev = gear.loadout.set(EquipSlot.RANGED, item)
        if (prev != null) gear.backpack.add(prev)
        val ars = gw.player[Arsenal]
        ars.curW = ars.weapons.indexOfFirst { it.def.id == item.weaponType }
    }

    @Test fun `mg and beam run on infinite ammo`() {
        assertTrue(Weapons.ALL.first { it.id == "mg" }.infiniteAmmo)
        assertTrue(Weapons.ALL.first { it.id == "beam" }.infiniteAmmo)
    }

    @Test fun `the machine gun carries a 120-round magazine`() {
        assertEquals(120, Weapons.ALL.first { it.id == "mg" }.magSize)
    }

    @Test fun `the new weapon types exist and are reachable through equipment`() {
        assertTrue(Weapons.ALL.any { it.id == "smg" && it.infiniteAmmo })
        assertTrue(Weapons.ALL.any { it.id == "rifle" })
        val input = InputState()
        val gw = world(input)
        // Drop everything else that fits RANGED so cycling lands on the rifle, then cycle it in.
        with(gw.world) {
            val gear = gw.player[Gear]
            gear.backpack.removeAll { it.id.startsWith("gun_") }
            gear.backpack.add(ItemCatalog.byId("gun_rifle")!!)
        }
        GearOps.cycleSlot(gw.world, gw.player, EquipSlot.RANGED)
        with(gw.world) {
            assertEquals("gun_rifle", gw.player[Gear].loadout.ranged?.id)
            assertEquals("rifle", gw.player[Arsenal].current.def.id) // GearOps pointed Arsenal at the new type
        }
    }

    @Test fun `the beam fires with an empty reserve`() {
        val input = InputState()
        val gw = world(input)
        with(gw.world) {
            gw.player[Ammo].ammoBeam = 0
            gw.player[Arsenal].curW = gw.player[Arsenal].weapons.indexOfFirst { it.def.id == "beam" }
        }
        input.fireRelease = true // beam is manual-fire: shoots on the release edge
        gw.world.update(1f / 60f)
        with(gw.world) {
            assertTrue(gw.player[Cooldowns].shoot > 0f, "the beam should have fired despite 0 reserve")
        }
    }

    @Test fun `a demolition shotgun's pellets carry its wall multiplier`() {
        val input = InputState()
        val gw = world(input)
        equipGun(gw, "gun_shotgun_3") // wallDmgMul 2.5
        input.fire = true
        gw.world.update(1f / 60f)
        val bullets = gw.world.family { all(Bullet) }
        assertTrue(bullets.numEntities > 0, "the shotgun should have fired")
        with(gw.world) { bullets.forEach { e -> assertEquals(2.5f, e[Bullet].wallMul, 1e-4f) } }
    }

    @Test fun `a wide-blast grenade carries its blast multiplier`() {
        val input = InputState()
        val gw = world(input)
        equipGun(gw, "gun_grenade_2") // blastMul 1.6
        input.fireRelease = true // grenade is manual-fire too
        gw.world.update(1f / 60f)
        val grenades = gw.world.family { all(Grenade) }
        assertTrue(grenades.numEntities > 0, "the grenade should have been thrown")
        with(gw.world) { grenades.forEach { e -> assertEquals(1.6f, e[Grenade].blastMul, 1e-4f) } }
    }

    @Test fun `a seeker pistol fires homing bullets`() {
        val input = InputState()
        val gw = world(input)
        with(gw.world) {
            val gear = gw.player[Gear]
            val prev = gear.loadout.set(EquipSlot.RANGED, ItemCatalog.byId("gun_pistol_seeker")!!)
            if (prev != null) gear.backpack.add(prev)
            gw.player[Arsenal].curW = gw.player[Arsenal].weapons.indexOfFirst { it.def.id == "pistol" }
        }
        input.fire = true
        gw.world.update(1f / 60f)
        val bullets = gw.world.family { all(Bullet) }
        assertTrue(bullets.numEntities > 0)
        with(gw.world) { bullets.forEach { e -> assertEquals(3f, e[Bullet].homing, 1e-4f) } }
    }

    @Test fun `broken walls occasionally hide a weapon cache`() {
        val gw = world(InputState())
        val rng = io.github.panda17tk.arpg.math.Rng(42L)
        repeat(300) { Pickups.dropOnWall(gw.world, rng, 500f, 500f) }
        var gunDrops = 0
        with(gw.world) {
            gw.world.family { all(io.github.panda17tk.arpg.ecs.components.Pickup) }.forEach { e ->
                val kind = e[io.github.panda17tk.arpg.ecs.components.Pickup].kind
                if (kind.startsWith("item:") && ItemCatalog.byId(kind.removePrefix("item:"))?.kind?.name == "RANGED_WEAPON") gunDrops++
            }
        }
        assertTrue(gunDrops in 1..30, "expected a few weapon caches out of 300 breaks, got $gunDrops")
    }

    @Test fun `number-key switching keeps an equipped graded gun of the same type`() {
        val input = InputState()
        val gw = world(input)
        equipGun(gw, "gun_mg_2")
        input.weaponSlot = with(gw.world) { gw.player[Arsenal] }.weapons.indexOfFirst { it.def.id == "mg" }
        gw.world.update(1f / 60f)
        with(gw.world) {
            assertEquals("gun_mg_2", gw.player[Gear].loadout.ranged?.id) // the grade survives re-selection
        }
    }

    @Test fun `number-key switching to another type equips an owned gun of that type`() {
        val input = InputState()
        val gw = world(input)
        equipGun(gw, "gun_mg_2") // starter gun_pistol went to the backpack
        input.weaponSlot = with(gw.world) { gw.player[Arsenal] }.weapons.indexOfFirst { it.def.id == "shotgun" }
        gw.world.update(1f / 60f)
        with(gw.world) {
            val gear = gw.player[Gear]
            assertEquals("gun_shotgun", gear.loadout.ranged?.id) // the owned starter shotgun swapped in
            assertTrue(gear.backpack.any { it.id == "gun_mg_2" }) // the graded mg went back to the backpack
        }
    }
}
