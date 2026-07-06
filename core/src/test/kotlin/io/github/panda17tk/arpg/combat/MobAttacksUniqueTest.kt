package io.github.panda17tk.arpg.combat

import com.github.quillraven.fleks.configureWorld
import io.github.panda17tk.arpg.config.AttackSpec
import io.github.panda17tk.arpg.config.EnemyDef
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.EBullet
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.MobAction
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.math.Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.atan2

/** v2.94 固有ボス技: the ring with a silence, the cutoff echo, the written wall. */
class MobAttacksUniqueTest {
    private val def = EnemyDef(name = "試験体", hp = 100f, speed = 10f)

    private fun fire(spec: AttackSpec, playerVx: Float = 0f, playerVy: Float = 0f): List<Pair<Transform, EBullet>> {
        val world = configureWorld { }
        val mobT = Transform(x = 0f, y = 0f)
        val executed = MobAttacks.tryAttack(
            world, spec, def, mobT, Facing(1f, 0f), Velocity(), MobAction(), Health(100f, 100f),
            Health(100f, 100f), Velocity(playerVx, playerVy),
            dist = 200f, toPx = 1f, toPy = 0f, see = true, iFrameMelee = 0.5f,
            config = GameConfig(), waveNum = 1, rng = Rng(7L),
        )
        assertTrue(executed, "the attack should fire")
        val out = ArrayList<Pair<Transform, EBullet>>()
        with(world) { world.family { all(EBullet, Transform) }.forEach { e -> out.add(e[Transform] to e[EBullet]) } }
        return out
    }

    @Test fun `the ring keeps one silence the keeper can slip through`() {
        val shots = fire(AttackSpec("ring_gap", count = 18, spread = 70f, speed = 150f, dmg = 10f, life = 2f))
        assertTrue(shots.size in 12..16, "a 70° silence in 18 shots removes 3-4 (got ${shots.size})")
        // the largest angular gap between neighbours must fit the promised silence
        val angles = shots.map { (_, b) -> atan2(b.vy, b.vx) }.sorted()
        var maxGap = 0.0f
        for (i in angles.indices) {
            val next = if (i == angles.size - 1) angles[0] + (Math.PI * 2).toFloat() else angles[i + 1]
            maxGap = maxOf(maxGap, next - angles[i])
        }
        assertTrue(maxGap >= Math.toRadians(60.0).toFloat(), "the silence is wide enough to dash through (got $maxGap)")
    }

    @Test fun `the echo lands ahead of a runner and sings back`() {
        // the player sits at (200, 0) running +x; the echo must seed further ahead and fly back (-x)
        val shots = fire(AttackSpec("cutoff_volley", count = 3, speed = 200f, dmg = 10f, life = 2f), playerVx = 120f)
        assertEquals(3, shots.size)
        for ((t, b) in shots) {
            assertTrue(t.x > 200f, "seeded ahead of the runner (x=${t.x})")
            assertTrue(b.vx < 0f, "singing back at them")
        }
    }

    @Test fun `a standing listener hears one aimed note instead`() {
        val shots = fire(AttackSpec("cutoff_volley", count = 3, speed = 200f, dmg = 10f, life = 2f))
        assertEquals(1, shots.size, "no motion to cut off — one aimed shot")
    }

    @Test fun `the written wall advances abreast with dodgeable edges`() {
        val shots = fire(AttackSpec("page_wall", count = 7, spread = 30f, speed = 130f, dmg = 12f, life = 3f))
        assertEquals(7, shots.size)
        for ((_, b) in shots) {
            assertTrue(b.vx > 0f && kotlin.math.abs(b.vy) < 1e-3f, "the whole line advances toward the player")
        }
        val ys = shots.map { (t, _) -> t.y }.sorted()
        assertEquals(-90f, ys.first(), 1e-3f)
        assertEquals(90f, ys.last(), 1e-3f)
    }

    @Test fun `the three signature moves are actually on their owners`() {
        val enemies = GameConfig().enemies
        assertTrue(enemies.getValue("rust_titan").attacks.any { it.type == "ring_gap" })
        assertTrue(enemies.getValue("chorus_node").attacks.any { it.type == "cutoff_volley" })
        assertTrue(enemies.getValue("archivist").attacks.any { it.type == "page_wall" })
    }
}
