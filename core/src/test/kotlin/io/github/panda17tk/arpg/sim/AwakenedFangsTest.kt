package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.systems.AISystem
import io.github.panda17tk.arpg.ecs.world.MobFactory
import io.github.panda17tk.arpg.ecs.world.WorldFactory
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.152 目覚めた牙: normals wield two moves, ranged brains kite, and the phase-2 rage endures. */
class AwakenedFangsTest {
    private val dt = 1f / 60f

    @Test fun `level one unlocks two moves — the utility opener no longer disarms`() {
        assertEquals(2, Leveling.attacksForLevel(1, 2), "a normal uses its damage move too")
        assertEquals(1, Leveling.attacksForLevel(1, 1), "single-move kinds are unchanged")
        assertEquals(3, Leveling.attacksForLevel(2, 5))
        assertEquals(0, Leveling.attacksForLevel(1, 0))
    }

    @Test fun `the once-toothless openers now bite the keeper`() {
        // stalker [blink, charge_melee] dealt ZERO damage before v2.152 — its blink was the whole kit
        val defs = GameConfig().enemies
        for (id in listOf("stalker", "shield_bearer", "null_hound", "patch_crab")) {
            val d = defs.getValue(id)
            assertTrue(d.attacks.size >= 2, "$id keeps its two moves")
            assertTrue(d.attacks.take(2).any { it.dmg > 0f || it.type == "charge_melee" || it.type == "melee" || it.type == "slam" },
                "$id has a damaging move within its unlocked pair")
        }
        val gw = WorldFactory.create(InputState(), seed = 3L)
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        MobFactory.spawn(gw.world, defs.getValue("shield_bearer"), px + 24f, py)
        val hp0 = with(gw.world) { gw.player[Health].hp }
        repeat(300) { gw.world.update(dt) } // five seconds toe to toe — the slam must land
        assertTrue(with(gw.world) { gw.player[Health].hp } < hp0, "the shield bearer's slam lands at last")
    }

    @Test fun `every ranged archetype counts for the kite brain`() {
        for (t in listOf("shot", "homing", "burst", "nova", "twin_shot", "barrage", "spray", "spiral")) {
            assertTrue(t in AISystem.RANGED_TYPES, "$t is ranged")
        }
        assertTrue("melee" !in AISystem.RANGED_TYPES && "guard" !in AISystem.RANGED_TYPES)
    }

    @Test fun `the underdog guns carry their new weight`() {
        val guns = io.github.panda17tk.arpg.combat.Weapons.ALL.associateBy { it.id }
        assertEquals(90f, guns.getValue("rifle").dmg, "finite ammo buys real damage now")
        assertEquals(10f, guns.getValue("smg").dmg)
        assertEquals(170f, guns.getValue("railgun").dmg)
    }
}
