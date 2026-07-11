package io.github.panda17tk.arpg.ecs.systems

import com.badlogic.gdx.graphics.Color
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.combat.MobDamage
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Boomerang
import io.github.panda17tk.arpg.ecs.components.Fx
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.MobAction
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.pathfinding.SpatialGrid
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.floor
import kotlin.math.hypot

/**
 * v2.101 帰還刃: flies the thrown blade — out on its own momentum (slowing), then home to the
 * keeper's hand. It cuts every mob it crosses once per leg; a wall turns it early. The return
 * leg ignores walls (the blade finds its keeper), and a hard life cap ends any impossible chase.
 */
class BoomerangSystem(private val mobGrid: SpatialGrid<Entity>) :
    IteratingSystem(family { all(Boomerang, Transform) }) {

    private val map: TileMap = world.inject()
    private val rng: Rng = world.inject()
    private val fx: Fx = world.inject()

    private var px = 0f
    private var py = 0f

    override fun onTick() {
        world.family { all(PlayerTag, Transform) }.forEach { e ->
            val t = e[Transform]; px = t.x; py = t.y
        }
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val t = entity[Transform]
        val b = entity[Boomerang]
        b.life -= deltaTime
        b.spin += SPIN_RATE * deltaTime
        if (b.life <= 0f) { world -= entity; return }

        if (!b.returning) {
            b.outT -= deltaTime
            val slow = 1f - OUT_DRAG * deltaTime
            b.vx *= slow; b.vy *= slow
            t.x += b.vx * deltaTime; t.y += b.vy * deltaTime
            val hitWall = map.solidAt(floor(t.x / Tuning.TILE).toInt(), floor(t.y / Tuning.TILE).toInt())
            if (b.outT <= 0f || hitWall) {
                if (hitWall) { // step back out of the wall so the turn is visible, then spark
                    t.x -= b.vx * deltaTime; t.y -= b.vy * deltaTime
                    fx.spawnChips(t.x, t.y, 4, chipColor)
                    fx.requestSfx("hit", 1.5f, 0.5f) // v2.106: the clang of the early turn
                }
                b.returning = true
                b.hit.clear() // the homeward leg cuts everyone afresh
            }
        } else {
            val dx = px - t.x; val dy = py - t.y
            val d = hypot(dx, dy)
            if (d < CATCH_DIST) { fx.requestSfx("blade_catch"); world -= entity; return } // back in hand (v2.106)
            val nx = if (d > 0f) dx / d else 1f; val ny = if (d > 0f) dy / d else 0f
            t.x += nx * RETURN_SPEED * deltaTime; t.y += ny * RETURN_SPEED * deltaTime
        }

        // The blade's edge: every mob it crosses is cut once per leg.
        mobGrid.forNearby(t.x, t.y, BLADE_RADIUS + io.github.panda17tk.arpg.sim.Tuning.MAX_BODY_HALF) { mobEntity ->
            if (mobEntity in b.hit) return@forNearby
            val mobT = with(world) { mobEntity[Transform] }
            val mobB = with(world) { mobEntity[Body] }
            val mobHalf = (mobB.halfW + mobB.halfH) * 0.5f
            if (hypot(mobT.x - t.x, mobT.y - t.y) > BLADE_RADIUS + mobHalf) return@forNearby
            b.hit.add(mobEntity)
            val mobH = with(world) { mobEntity[Health] }
            val mobV = with(world) { mobEntity[Velocity] }
            val mobA = with(world) { mobEntity[MobAction] }
            val mobDodge = with(world) { mobEntity[Mob].def.dodge }
            val ddx = mobT.x - t.x; val ddy = mobT.y - t.y
            val dd = hypot(ddx, ddy)
            val knx = if (dd > 0f) ddx / dd else 1f
            val kny = if (dd > 0f) ddy / dd else 0f
            if (MobDamage.hurt(mobH, mobV, mobA, mobDodge, b.dmg, knx, kny, 190f, rng.nextFloat())) {
                fx.spawnPop(mobT.x, mobT.y - mobB.halfH - 6f, b.dmg.toInt(), popBlade)
            }
        }
    }

    companion object {
        const val OUT_TIME = 0.55f       // seconds of outbound flight before the turn
        const val CATCH_DIST = 26f       // the hand's reach
        const val BLADE_RADIUS = 26f     // the spinning edge's bite
        private const val OUT_DRAG = 1.4f
        private const val RETURN_SPEED = 620f
        private const val SPIN_RATE = 14f
        private val popBlade = Color.valueOf("cfe8d8") // pale jade — the blade's own numbers
        private val chipColor = Color.valueOf("8a8076")
    }
}
