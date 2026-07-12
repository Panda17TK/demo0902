package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.combat.Weapons
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Boomerang
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.item.ItemCatalog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.101 新武器: the railgun's placed pierce and the boomerang blade's two-leg cut. */
class WorldNewWeaponsTest {
    /** A big, quiet target: no attacks, no dodge, too much hp to die mid-test. */
    private fun tankDef() = GameConfig().enemies.getValue("zombie").copy(attacks = emptyList(), hp = 5000f)

    private fun slotOf(id: String) = Weapons.ALL.indexOfFirst { it.id == id }.also { assertTrue(it >= 0, "$id exists") }

    @Test fun `the roster carries both newcomers with their signatures`() {
        val rail = Weapons.ALL.first { it.id == "railgun" }
        assertTrue(rail.manualFire, "置き撃ち — aim, then release")
        assertTrue(rail.reloadTime >= 3f, "a sniper's reload")
        val blade = Weapons.ALL.first { it.id == "blade" }
        assertEquals(null, blade.magSize, "the blade IS the ammunition")
        assertTrue(blade.infiniteAmmo)
        // both are reachable as gear, so they drop and sell like every other gun
        assertTrue(ItemCatalog.ALL.any { it.weaponType == "railgun" })
        assertTrue(ItemCatalog.ALL.any { it.weaponType == "blade" })
        // v2.181 静かな拾い手: the magnetic gun rides the existing trait plumbing
        val gatherer = ItemCatalog.ALL.first { it.id == "gun_smg_gatherer" }
        assertTrue(io.github.panda17tk.arpg.item.ItemTrait.MAGNET in gatherer.traits)
    }

    @Test fun `the railgun slug pierces everything on the line, undiminished`() {
        val input = InputState()
        val gw = WorldFactory.create(input, seed = 1L)
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        val near = MobFactory.spawn(gw.world, tankDef(), px + 220f, py)
        val far = MobFactory.spawn(gw.world, tankDef(), px + 900f, py)
        input.weaponSlot = slotOf("railgun")
        gw.world.update(1f / 60f)
        input.weaponSlot = -1
        input.fireRelease = true
        gw.world.update(1f / 60f)
        input.fireRelease = false
        val nearHp = with(gw.world) { near[Health].hp }
        val farHp = with(gw.world) { far[Health].hp }
        assertTrue(nearHp < 5000f, "the near target is hit")
        assertTrue(farHp < 5000f, "the slug carries on through to the far target")
        assertEquals(nearHp, farHp, 0.01f, "piercing loses nothing along the way")
    }

    @Test fun `a settled aim doubles the slug`() {
        fun dmgAfter(chargeTicks: Int): Float {
            val input = InputState()
            val gw = WorldFactory.create(input, seed = 1L)
            val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
            val tank = MobFactory.spawn(gw.world, tankDef(), px + 220f, py)
            input.weaponSlot = slotOf("railgun")
            gw.world.update(1f / 60f)
            input.weaponSlot = -1
            input.aiming = true
            repeat(chargeTicks) { gw.world.update(1f / 60f) }
            input.aiming = false
            input.fireRelease = true
            gw.world.update(1f / 60f)
            input.fireRelease = false
            return 5000f - with(gw.world) { tank[Health].hp }
        }
        val snap = dmgAfter(0)
        val settled = dmgAfter(70) // > RAIL_CHARGE_TIME (1.0s) of held aim
        assertTrue(snap > 0f, "even a snap shot lands ($snap)")
        assertEquals(2f, settled / snap, 0.1f, "full charge pays double (snap $snap → settled $settled)")
    }

    @Test fun `the blade cuts going out, comes home, and never doubles up in flight`() {
        val input = InputState()
        val gw = WorldFactory.create(input, seed = 1L)
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        val tank = MobFactory.spawn(gw.world, tankDef(), px + 150f, py)
        input.weaponSlot = slotOf("blade")
        gw.world.update(1f / 60f)
        input.weaponSlot = -1
        input.fire = true
        val blades = gw.world.family { all(Boomerang) }
        repeat(30) { // 0.5s of held trigger: past the 0.35s cooldown, still only one blade out
            gw.world.update(1f / 60f)
            assertTrue(blades.numEntities <= 1, "one blade, out and back — never two")
        }
        input.fire = false
        assertEquals(1, blades.numEntities, "the blade is in flight")
        val hpOut = with(gw.world) { tank[Health].hp }
        assertTrue(hpOut < 5000f, "cut on the way out")
        repeat(240) { gw.world.update(1f / 60f) } // 4s: turn + fly home
        assertEquals(0, blades.numEntities, "the blade came home to the hand")
    }

    @Test fun `the tuning catalog reaches the newcomers too`() {
        val config = GameConfig()
        val params = io.github.panda17tk.arpg.config.TuningCatalog.paramsFor(config)
        assertTrue(params.any { it.name == "レールガン 威力" })
        assertTrue(params.any { it.name == "帰還刃 威力" })
    }
}
