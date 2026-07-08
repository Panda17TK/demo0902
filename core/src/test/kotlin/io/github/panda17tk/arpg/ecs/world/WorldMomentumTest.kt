package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.sim.Tuning
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.127 ダッシュの重み: a fast ram shoves mobs; a hard slam costs breath as well as hull. */
class WorldMomentumTest {
    private val dt = 1f / 60f
    private fun tankDef() = GameConfig().enemies.getValue("zombie").copy(attacks = emptyList(), hp = 5000f)

    @Test fun `a dashing player at speed shoves the mob it rams — a from-rest tap does not`() {
        // at speed: drift carries real momentum into the contact
        val fast = WorldFactory.create(InputState(), seed = 1L)
        val (px, py) = with(fast.world) { fast.player[Transform].let { it.x to it.y } }
        val mobF = MobFactory.spawn(fast.world, tankDef(), px + 24f, py)
        with(fast.world) { fast.player[Velocity].driftX = 420f }
        fast.gw().input.dash = true
        fast.world.update(dt)
        val shoved = with(fast.world) { mobF[Velocity].vx }
        assertTrue(shoved > 150f, "a fast ram flings the mob away (got $shoved)")

        // from rest: the same tap has no shove in it yet
        val slow = WorldFactory.create(InputState(), seed = 1L)
        val (qx, qy) = with(slow.world) { slow.player[Transform].let { it.x to it.y } }
        val mobS = MobFactory.spawn(slow.world, tankDef(), qx + 24f, qy)
        slow.gw().input.dash = true
        slow.world.update(dt)
        val still = with(slow.world) { mobS[Velocity].vx }
        assertTrue(still < 150f, "a from-rest dash tap must not bat the mob aside (got $still)")
    }

    @Test fun `a damaging slam costs two stamina segments — a gentle bump costs none`() {
        val gw = WorldFactory.create(InputState(), seed = 2L)
        // find a solid tile with open floor on its left, and stand there facing it
        var wall: Pair<Int, Int>? = null
        outer@ for (ty in 1 until gw.map.height - 1) {
            for (tx in 2 until gw.map.width - 1) {
                if (gw.map.solidAt(tx, ty) && !gw.map.solidAt(tx - 1, ty)) { wall = tx to ty; break@outer }
            }
        }
        val (tx, ty) = wall ?: error("no wall found in the space stage")
        with(gw.world) {
            val t = gw.player[Transform]
            t.x = (tx - 1 + 0.5f) * Tuning.TILE; t.y = (ty + 0.5f) * Tuning.TILE
            t.prevX = t.x; t.prevY = t.y
            gw.player[Velocity].driftX = 420f // well past the damage threshold (320)
        }
        val staBefore = with(gw.world) { gw.player[Stamina].value }
        val hpBefore = with(gw.world) { gw.player[Health].hp }
        repeat(8) { gw.world.update(dt) }
        val sta = with(gw.world) { gw.player[Stamina].value }
        val hp = with(gw.world) { gw.player[Health].hp }
        assertTrue(hp < hpBefore, "the slam must hurt (hp $hpBefore -> $hp)")
        // two segments minus a few ticks of natural regen after the hit
        assertTrue(sta <= staBefore - staBefore * 2f / 12f + 8f, "two segments of breath gone (sta $staBefore -> $sta)")

        // gentle: under the damage threshold the wind stays in
        val calm = WorldFactory.create(InputState(), seed = 2L)
        with(calm.world) {
            val t = calm.player[Transform]
            t.x = (tx - 1 + 0.5f) * Tuning.TILE; t.y = (ty + 0.5f) * Tuning.TILE
            t.prevX = t.x; t.prevY = t.y
            calm.player[Velocity].driftX = 250f // shake territory, no damage
        }
        repeat(8) { calm.world.update(dt) }
        val calmSta = with(calm.world) { calm.player[Stamina].value }
        val calmMax = with(calm.world) { calm.player[Stamina].max }
        assertTrue(calmSta > calmMax * 0.95f, "a gentle bump costs no breath (got $calmSta/$calmMax)")
    }

    // InputState is captured by the world at creation; reach it back for the dash flag.
    private fun GameWorld.gw(): GameWorld = this
    private val GameWorld.input: InputState get() = world.inject()
}
