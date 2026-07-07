package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.combat.Weapons
import io.github.panda17tk.arpg.ecs.components.Cooldowns
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.106 音の第3弾: the guns speak through the sim's sfx queue, and a charge never crosses weapons. */
class WorldWeaponAudioTest {
    private fun slotOf(id: String) = Weapons.ALL.indexOfFirst { it.id == id }

    private fun switchTo(gw: GameWorld, input: InputState, id: String) {
        input.weaponSlot = slotOf(id)
        gw.world.update(1f / 60f)
        input.weaponSlot = -1
    }

    @Test fun `firing queues the weapon's voice for the screen`() {
        val input = InputState().apply { fire = true }
        val gw = WorldFactory.create(input, seed = 1L)
        gw.world.update(1f / 60f)
        val names = gw.fx.drainSfx().map { it.name }
        assertTrue("shot" in names, "the pistol speaks (got $names)")
    }

    @Test fun `the railgun thunks and the blade whooshes then clicks home`() {
        val input = InputState()
        val gw = WorldFactory.create(input, seed = 1L)
        switchTo(gw, input, "railgun")
        gw.fx.drainSfx()
        input.fireRelease = true
        gw.world.update(1f / 60f)
        input.fireRelease = false
        assertTrue(gw.fx.drainSfx().any { it.name == "rail" }, "the slug's thunk")
        repeat(110) { gw.world.update(1f / 60f) } // the rail's 1.6s cooldown clears before the throw
        switchTo(gw, input, "blade")
        gw.fx.drainSfx()
        input.fire = true
        gw.world.update(1f / 60f)
        input.fire = false
        assertTrue(gw.fx.drainSfx().any { it.name == "blade_throw" }, "the throw's whoosh")
        val heard = mutableSetOf<String>()
        repeat(240) { gw.world.update(1f / 60f); gw.fx.drainSfx().forEach { heard.add(it.name) } }
        assertTrue("blade_catch" in heard, "the catch clicks (heard $heard)")
    }

    @Test fun `a settled rail aim chimes exactly once per charge cycle`() {
        val input = InputState()
        val gw = WorldFactory.create(input, seed = 1L)
        switchTo(gw, input, "railgun")
        gw.fx.drainSfx()
        input.aiming = true
        var chimes = 0
        repeat(90) { gw.world.update(1f / 60f); chimes += gw.fx.drainSfx().count { it.name == "rail_ready" } }
        input.aiming = false
        assertEquals(1, chimes, "one chime when the aim settles, then silence")
    }

    @Test fun `a beam charge does not carry into the railgun`() {
        val input = InputState()
        val gw = WorldFactory.create(input, seed = 1L)
        switchTo(gw, input, "beam")
        input.aiming = true
        repeat(90) { gw.world.update(1f / 60f) } // full beam charge (1.4s needed, 1.5s given)
        input.aiming = false
        assertTrue(with(gw.world) { gw.player[Cooldowns].beamCharge } >= 0.9f, "the beam charged")
        switchTo(gw, input, "railgun")
        gw.world.update(1f / 60f) // the ownership check runs on the first railgun tick
        assertEquals(0f, with(gw.world) { gw.player[Cooldowns].beamCharge }, 1e-4f, "the carry is dropped")
    }

    @Test fun `pickups answer with a small voice`() {
        val input = InputState()
        val gw = WorldFactory.create(input, seed = 1L)
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        Pickups.spawn(gw.world, "dust", 5, px, py)
        gw.fx.drainSfx()
        gw.world.update(1f / 60f)
        assertTrue(gw.fx.drainSfx().any { it.name == "pickup" }, "星屑 blips on collection")
    }
}
