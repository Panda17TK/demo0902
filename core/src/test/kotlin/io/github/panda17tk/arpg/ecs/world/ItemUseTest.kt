package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Buff
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.ecs.components.Smoke
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.item.ItemCatalog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ItemUseTest {
    private fun world() = WorldFactory.create(InputState(), seed = 3L)

    @Test fun `a med heals but is refused at full HP`() {
        val gw = world()
        with(gw.world) {
            val h = gw.player[Health]
            assertNull(ItemUse.use(gw.world, gw.player, ItemCatalog.byId("med_spray")!!)) // full HP → wasted → refused
            h.hp = 10f
            assertNotNull(ItemUse.use(gw.world, gw.player, ItemCatalog.byId("med_spray")!!))
            assertEquals(35f, h.hp, 1e-3f)
            h.hp = h.hpMax - 5f // healing clamps at max
            ItemUse.use(gw.world, gw.player, ItemCatalog.byId("med_kit")!!)
            assertEquals(h.hpMax, h.hp, 1e-3f)
        }
    }

    @Test fun `stimulants set the matching buff timers`() {
        val gw = world()
        with(gw.world) {
            ItemUse.use(gw.world, gw.player, ItemCatalog.byId("stim_feather")!!)
            ItemUse.use(gw.world, gw.player, ItemCatalog.byId("stim_dash")!!)
            assertEquals(6f, gw.player[Buff].staminaInfT, 1e-3f)
            assertEquals(8f, gw.player[Buff].dashUpT, 1e-3f)
        }
    }

    @Test fun `the stamina drink refills and is refused when already full`() {
        val gw = world()
        with(gw.world) {
            val s = gw.player[Stamina]
            assertNull(ItemUse.use(gw.world, gw.player, ItemCatalog.byId("sta_drink")!!))
            s.value = 1f
            assertNotNull(ItemUse.use(gw.world, gw.player, ItemCatalog.byId("sta_drink")!!))
            assertEquals(s.max, s.value, 1e-3f)
        }
    }

    @Test fun `supplies grant blocks and ammo`() {
        val gw = world()
        with(gw.world) {
            val blocksBefore = gw.player[Materials].blocks
            val ammoBefore = gw.player[Ammo].ammo9
            ItemUse.use(gw.world, gw.player, ItemCatalog.byId("repair_pack")!!)
            ItemUse.use(gw.world, gw.player, ItemCatalog.byId("ammo_cache")!!)
            assertEquals(blocksBefore + 3, gw.player[Materials].blocks)
            assertEquals(ammoBefore + 20, gw.player[Ammo].ammo9)
        }
    }

    @Test fun `the smoke bomb spawns a cloud at the player`() {
        val gw = world()
        assertNotNull(ItemUse.use(gw.world, gw.player, ItemCatalog.byId("smoke_bomb")!!))
        assertEquals(1, gw.world.family { all(Smoke) }.numEntities)
    }

    @Test fun `timed resistances set their buff timers`() {
        val gw = world()
        with(gw.world) {
            assertNotNull(ItemUse.use(gw.world, gw.player, ItemCatalog.byId("coat_heat")!!))
            assertNotNull(ItemUse.use(gw.world, gw.player, ItemCatalog.byId("coat_cold")!!))
            assertNotNull(ItemUse.use(gw.world, gw.player, ItemCatalog.byId("field_magnet")!!))
            assertNotNull(ItemUse.use(gw.world, gw.player, ItemCatalog.byId("patch_regen")!!))
            val bf = gw.player[Buff]
            assertEquals(60f, bf.heatProofT, 1e-3f)
            assertEquals(60f, bf.coldProofT, 1e-3f)
            assertEquals(45f, bf.magnetT, 1e-3f)
            assertEquals(20f, bf.regenT, 1e-3f)
        }
    }

    @Test fun `equipment and lore are not usable`() {
        val gw = world()
        assertNull(ItemUse.use(gw.world, gw.player, ItemCatalog.byId("acc_boots")!!))
        assertNull(ItemUse.use(gw.world, gw.player, ItemCatalog.byId("lore_letter")!!))
    }

    @Test fun `lore drops resolve to readable items`() {
        val lore = ItemCatalog.dropFor(900)
        assertTrue(lore.lore.lines().size >= 2) // multi-line readables, not one-liners
    }
}
