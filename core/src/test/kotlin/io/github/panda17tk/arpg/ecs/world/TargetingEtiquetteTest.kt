package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Gear
import io.github.panda17tk.arpg.ecs.components.Grenade
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.item.EquipSlot
import io.github.panda17tk.arpg.item.ItemCatalog
import io.github.panda17tk.arpg.sim.Predation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.137 狙いの節度: auto-targeting ignores harmless wildlife; the leech never drinks from fish. */
class TargetingEtiquetteTest {
    private val dt = 1f / 60f

    @Test fun `auto-targeting locks threats, never harmless wildlife`() {
        val defs = GameConfig().enemies
        assertFalse(Predation.autoTargetable(defs.getValue("star_sardine")), "a sardine earns no lock")
        assertFalse(Predation.autoTargetable(defs.getValue("isle_whale")), "the gentle island earns no lock")
        assertFalse(Predation.autoTargetable(defs.getValue("horn_deer")), "a grazer earns no lock")
        assertTrue(Predation.autoTargetable(defs.getValue("tyrant_shark")), "the tyrant hunts the keeper — lockable")
        assertTrue(Predation.autoTargetable(defs.getValue("zombie")), "hostiles always lock")
    }

    @Test fun `a passing minnow does not trip the grenade's proximity fuse — a hostile does`() {
        val gw = WorldFactory.create(InputState(), seed = 4L)
        val defs = GameConfig().enemies
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }

        // a fish sits right on the fuse — nothing happens
        MobFactory.spawn(gw.world, defs.getValue("star_sardine"), px + 150f, py)
        gw.world.entity {
            it += Transform(x = px + 150f, y = py)
            it += Grenade(0f, 0f, fuse = 30f, blastMul = 1f)
        }
        repeat(5) { gw.world.update(dt) }
        assertEquals(1, gw.world.family { all(Grenade) }.numEntities, "the minnow must not trip the fuse")

        // a hostile walks onto the same fuse — it blows
        MobFactory.spawn(gw.world, defs.getValue("zombie"), px + 150f, py)
        repeat(5) { gw.world.update(dt) }
        assertEquals(0, gw.world.family { all(Grenade) }.numEntities, "a hostile trips it")
    }

    @Test fun `the leech blade never drinks from a fish`() {
        val input = InputState()
        val gw = WorldFactory.create(input, seed = 5L)
        val defs = GameConfig().enemies
        with(gw.world) {
            gw.player[Gear].loadout.set(EquipSlot.MELEE, ItemCatalog.byId("melee_leech")!!)
            gw.player[Health].hp = 40f
            gw.player[Facing].x = 1f; gw.player[Facing].y = 0f
        }
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        MobFactory.spawn(gw.world, defs.getValue("star_sardine"), px + 30f, py)
        input.melee = true
        gw.world.update(dt)
        input.melee = false
        gw.world.update(dt) // reap tick
        // v2.157: a landmark flock may drift extra sardines into the arc — the invariant is
        // "the swing lands AND drinks nothing", not the exact body count.
        assertTrue((gw.gameOver.killsByKind["star_sardine"] ?: 0) >= 1, "the swing landed and felled the fish")
        val hp = with(gw.world) { gw.player[Health].hp }
        assertTrue(hp <= 40f + 0.001f, "no healing was drunk from wildlife (hp $hp)")
    }
}
