package io.github.panda17tk.arpg.combat

import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import io.github.panda17tk.arpg.config.AttackSpec
import io.github.panda17tk.arpg.config.EnemyDef
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.EBullet
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.MobAction
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.math.Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MobAttacksTest {
    private val def = EnemyDef(name = "boss", tier = "boss", hp = 1000f, speed = 50f)

    /** Invoke a boss attack against a player 200px to the +x with the given visibility. */
    private fun fire(
        world: World,
        spec: AttackSpec,
        mobH: Health = Health(1000f, 1000f),
        action: MobAction = MobAction(),
        see: Boolean = true,
    ): Boolean = MobAttacks.tryAttack(
        world, spec, def, Transform(x = 0f, y = 0f), Facing(), Velocity(), action,
        mobH, Health(100f, 100f), Velocity(),
        dist = 200f, toPx = 1f, toPy = 0f, see = see, iFrameMelee = 0.9f,
        config = GameConfig(), waveNum = 1, rng = Rng(1L),
    )

    @Test fun `nova spawns count bullets in all directions`() {
        val w = configureWorld {}
        assertTrue(fire(w, AttackSpec("nova", count = 8, speed = 180f, dmg = 8f)))
        assertEquals(8, w.family { all(EBullet) }.numEntities)
    }

    @Test fun `burst needs line of sight`() {
        val w = configureWorld {}
        assertFalse(fire(w, AttackSpec("burst", count = 5, spread = 40f, speed = 240f), see = false))
        assertEquals(0, w.family { all(EBullet) }.numEntities)
        assertTrue(fire(w, AttackSpec("burst", count = 5, spread = 40f, speed = 240f), see = true))
        assertEquals(5, w.family { all(EBullet) }.numEntities)
    }

    @Test fun `summon spawns minions of the named kind`() {
        val w = configureWorld {}
        assertTrue(fire(w, AttackSpec("summon", minion = "zombie", count = 3)))
        assertEquals(3, w.family { all(Mob) }.numEntities)
    }

    @Test fun `heal restores mob hp and stops at full`() {
        val w = configureWorld {}
        val h = Health(50f, 100f)
        assertTrue(fire(w, AttackSpec("heal", amount = 30f), mobH = h))
        assertEquals(80f, h.hp, 1e-3f)
        assertFalse(fire(w, AttackSpec("heal", amount = 30f), mobH = Health(100f, 100f)))
    }

    @Test fun `enrage sets action timers`() {
        val w = configureWorld {}
        val a = MobAction()
        assertTrue(fire(w, AttackSpec("enrage", mul = 1.6f, duration = 4f), action = a))
        assertEquals(4f, a.enrageT, 1e-3f); assertEquals(1.6f, a.enrageMul, 1e-3f)
    }

    @Test fun `mine spawns one stationary bullet`() {
        val w = configureWorld {}
        assertTrue(fire(w, AttackSpec("mine", dmg = 18f, life = 6f)))
        val bullets = w.family { all(EBullet) }
        assertEquals(1, bullets.numEntities)
        bullets.forEach { e -> with(w) { assertTrue(e[EBullet].mine); assertEquals(0f, e[EBullet].vx, 1e-3f) } }
    }
}
