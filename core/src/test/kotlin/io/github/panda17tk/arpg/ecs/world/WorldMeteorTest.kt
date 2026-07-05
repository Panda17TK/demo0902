package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Meteor
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Pickup
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.sim.WaveEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.87 流星群: METEOR waves rain telegraphed rocks that land, hurt, and shed dust. */
class WorldMeteorTest {
    private fun clearMobs(gw: GameWorld) {
        val doomed = ArrayList<com.github.quillraven.fleks.Entity>()
        with(gw.world) { gw.world.family { all(Mob) }.forEach { doomed.add(it) } }
        for (e in doomed) gw.world -= e
    }

    @Test fun `a meteor wave rains rocks that fall into dust`() {
        val gw = WorldFactory.create(InputState(), seed = 3L)
        // Fast-forward the surge to wave 6 — the first METEOR slot (6 ≡ 6 mod 8).
        gw.waveState.num = 5
        gw.waveState.phase = "intermission"
        gw.waveState.interT = 0.01f
        gw.waveState.toSpawn = 0
        var sawFalling = false
        repeat((60 * 2.0f).toInt()) {
            clearMobs(gw)
            gw.world.update(1f / 60f)
            if (gw.world.family { all(Meteor) }.numEntities > 0) sawFalling = true
        }
        assertEquals(6, gw.waveState.num)
        assertEquals(WaveEvent.METEOR, gw.waveState.event, "wave 6 carries the meteor rain")
        assertTrue(sawFalling, "rocks must telegraph their fall")
        var dust = 0
        with(gw.world) {
            gw.world.family { all(Pickup) }.forEach { e -> if (e[Pickup].kind == "dust") dust++ }
        }
        assertTrue(dust > 0, "an impact sheds dust pickups")
    }
}
