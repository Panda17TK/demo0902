package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Pickup
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** v2.141 図鑑の救済: the keeper's whale-class hunt pays dust — the ecosystem's own kills still pay nothing. */
class GiantBountyTest {
    private val dt = 1f / 60f

    private fun dustNear(gw: GameWorld, x: Float, y: Float): Int {
        var dust = 0
        with(gw.world) {
            gw.world.family { all(Pickup, Transform) }.forEach { e ->
                val t = e[Transform]
                if (e[Pickup].kind == "dust" && kotlin.math.hypot((t.x - x).toDouble(), (t.y - y).toDouble()) < 100.0) {
                    dust += e[Pickup].amount
                }
            }
        }
        return dust
    }

    @Test fun `the keeper's whale kill pays dust - the wild's own does not`() {
        val gw = WorldFactory.create(InputState(), seed = 8L)
        val whaleDef = GameConfig().enemies.getValue("isle_whale")
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }

        val mine = MobFactory.spawn(gw.world, whaleDef, px + 500f, py)
        with(gw.world) { mine[Health].hp = 0f } // the keeper's kill
        gw.world.update(dt)
        assertEquals(30, dustNear(gw, px + 500f, py), "the whale-class hunt pays 30 dust")

        val hunted = MobFactory.spawn(gw.world, whaleDef, px - 500f, py)
        with(gw.world) { hunted[Mob].fellByWild = true; hunted[Health].hp = 0f } // the wild's deed
        gw.world.update(dt)
        assertEquals(0, dustNear(gw, px - 500f, py), "the ecosystem's own kill still pays nothing")
    }
}
