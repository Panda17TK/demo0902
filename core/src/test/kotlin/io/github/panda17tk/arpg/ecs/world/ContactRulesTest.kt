package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.PlayerConfig
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

/**
 * Contact rules: mere touch never hurts the player (only explicit attacks do), and a mob slamming
 * in with dash/fling momentum knocks the player back (the rammed side takes the knockback).
 */
class ContactRulesTest {
    /** A zombie with its attacks stripped: the only player interaction left is body contact. */
    private fun touchOnlyZombie() = GameConfig().enemies.getValue("zombie").copy(attacks = emptyList())

    @Test fun `mere contact with a hostile mob deals no damage to the player`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        MobFactory.spawn(gw.world, touchOnlyZombie(), px + 8f, py)
        val hpBefore = with(gw.world) { gw.player[Health].hp }
        gw.world.update(1f / 60f)
        val hpAfter = with(gw.world) { gw.player[Health].hp }
        assertEquals(hpBefore, hpAfter, 0.0001f, "touching a mob must not damage the player")
    }

    @Test fun `mere contact shoves neither side`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        val mob = MobFactory.spawn(gw.world, touchOnlyZombie(), px + 8f, py)
        gw.world.update(1f / 60f)
        val pv = with(gw.world) { gw.player[Velocity] }
        assertTrue(abs(pv.vx) < 50f && abs(pv.vy) < 50f, "no ram → no player knockback (${pv.vx}, ${pv.vy})")
        val mv = with(gw.world) { mob[Velocity] }
        assertTrue(abs(mv.vx) < 50f, "a merely-touching mob is free to press in for melee (vx=${mv.vx})")
    }

    @Test fun `a mob ramming in with dash momentum knocks the player back`() {
        val gw = WorldFactory.create(InputState(), seed = 1L)
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        val mob = MobFactory.spawn(gw.world, touchOnlyZombie(), px + 8f, py)
        with(gw.world) { mob[Velocity].driftX = -300f } // a dash burst carrying it into the player
        gw.world.update(1f / 60f)
        val pv = with(gw.world) { gw.player[Velocity] }
        assertTrue(pv.vx < -100f, "the rammed player should be knocked back along the mob's motion (vx=${pv.vx})")
        val hp = with(gw.world) { gw.player[Health].hp }
        assertEquals(GameConfig().player.hpMax, hp, 0.0001f, "a ram knocks back but never damages")
    }

    @Test fun `gun range is doubled`() {
        val p = PlayerConfig()
        // Bullet range = speed × life. Doubled from the legacy 360 × 0.9 = 324 to 648.
        assertEquals(648f, p.bulletSpeed * p.bulletLife, 0.01f)
    }
}
