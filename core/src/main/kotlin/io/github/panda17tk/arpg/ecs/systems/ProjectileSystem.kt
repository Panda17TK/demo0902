package io.github.panda17tk.arpg.ecs.systems

import com.badlogic.gdx.graphics.Color
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.combat.Ballistics
import io.github.panda17tk.arpg.combat.Explosion
import io.github.panda17tk.arpg.combat.MobDamage
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Bullet
import io.github.panda17tk.arpg.ecs.components.Fx
import io.github.panda17tk.arpg.ecs.components.Grenade
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.MobAction
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.ecs.world.Pickups
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.map.Tiles
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.pathfinding.FlowField
import io.github.panda17tk.arpg.pathfinding.SpatialGrid
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin

/** Advances player bullets and grenades; breaks walls; explodes grenades; damages mobs (with dodge). */
class ProjectileSystem(private val mobGrid: SpatialGrid<Entity>) :
    IteratingSystem(family { any(Bullet, Grenade) }) {

    private val map: TileMap = world.inject()
    private val flow: FlowField = world.inject()
    private val config: GameConfig = world.inject()
    private val rng: Rng = world.inject()
    private val fx: Fx = world.inject()
    private val chipColor = Color.valueOf("8a8076")

    private var pPlayerX = 0
    private var pPlayerY = 0

    private fun playerTileX(): Int = pPlayerX
    private fun playerTileY(): Int = pPlayerY

    override fun onTick() {
        world.family { all(PlayerTag, Transform) }.forEach { e ->
            val t = e[Transform]
            pPlayerX = floor(t.x / Tuning.TILE).toInt()
            pPlayerY = floor(t.y / Tuning.TILE).toInt()
        }
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val t = entity[Transform]
        if (Bullet in entity) {
            val b = entity[Bullet]
            // v2.40: seeker rounds bend toward the nearest enemy in range (mirrors EBullet homing).
            if (b.homing > 0f) {
                var bestD = Float.MAX_VALUE; var bestX = 0f; var bestY = 0f
                mobGrid.forNearby(t.x, t.y, HOMING_RANGE) { mobEntity ->
                    val mt = with(world) { mobEntity[Transform] }
                    val d = hypot(mt.x - t.x, mt.y - t.y)
                    if (d < bestD) { bestD = d; bestX = mt.x; bestY = mt.y }
                }
                if (bestD < HOMING_RANGE) {
                    val twoPi = (Math.PI * 2.0).toFloat(); val pi = Math.PI.toFloat()
                    val want = atan2(bestY - t.y, bestX - t.x)
                    val cur = atan2(b.vy, b.vx)
                    val diff = ((want - cur + pi * 3f) % twoPi) - pi
                    val turn = diff.coerceIn(-b.homing * deltaTime, b.homing * deltaTime)
                    val sp = hypot(b.vx, b.vy)
                    val na = cur + turn
                    b.vx = cos(na) * sp; b.vy = sin(na) * sp
                }
            }
            val step = Ballistics.stepBullet(map, t.x, t.y, b.vx, b.vy, b.life, deltaTime)
            t.x = step.x; t.y = step.y; b.life = step.life

            // --- Bullet vs mob hit check (before wall despawn) ---
            var mobHit = false
            mobGrid.forNearby(t.x, t.y, 24f) { mobEntity ->
                if (mobHit) return@forNearby
                val mobT = with(world) { mobEntity[Transform] }
                val mobB = with(world) { mobEntity[Body] }
                if (abs(mobT.x - t.x) < mobB.halfW && abs(mobT.y - t.y) < mobB.halfH) {
                    val mobH = with(world) { mobEntity[Health] }
                    val mobV = with(world) { mobEntity[Velocity] }
                    val mobA = with(world) { mobEntity[MobAction] }
                    val mobDodge = with(world) { mobEntity[Mob].def.dodge }
                    val ddx = mobT.x - t.x; val ddy = mobT.y - t.y
                    val dist = hypot(ddx, ddy)
                    val nx = if (dist > 0f) ddx / dist else 1f
                    val ny = if (dist > 0f) ddy / dist else 0f
                    MobDamage.hurt(mobH, mobV, mobA, mobDodge, b.dmg, nx, ny, 160f, rng.nextFloat())
                    mobHit = true
                }
            }
            if (mobHit) { world -= entity; return }

            val wall = step.wallTile
            if (wall != null) {
                fx.spawnChips(t.x, t.y, 5, chipColor)
                // v2.37: demolition-grade guns chew through blocks faster (wallMul from the equipped gun).
                if (Tiles.damageTile(map, wall.first, wall.second, b.dmg * b.wallMul).broke) {
                    flow.rebuild(map, playerTileX(), playerTileY(), FlowRebuildSystem.MAX_DIST)
                    Pickups.dropOnWall(world, rng, (wall.first + 0.5f) * Tuning.TILE, (wall.second + 0.5f) * Tuning.TILE)
                }
                world -= entity
                return
            }
            if (step.expired) world -= entity

        } else {
            val g = entity[Grenade]
            t.x += g.vx * deltaTime; t.y += g.vy * deltaTime; g.fuse -= deltaTime
            val tx = floor(t.x / Tuning.TILE).toInt(); val ty = floor(t.y / Tuning.TILE).toInt()
            // proximity: an enemy within ~1 tile triggers a bigger (2-tile) blast immediately
            var prox = false
            mobGrid.forNearby(t.x, t.y, Tuning.TILE) { mobEntity ->
                if (!prox) {
                    val mt = with(world) { mobEntity[Transform] }
                    if (hypot(mt.x - t.x, mt.y - t.y) <= Tuning.TILE) prox = true
                }
            }
            if (map.solidAt(tx, ty) || g.fuse <= 0f || prox) {
                // v2.37: the grenade's grade widens the whole blast (mob damage + wall crater alike).
                detonate(t.x, t.y, maxOf(Tuning.TILE * 2f, config.player.explodeRadius) * g.blastMul, g.blastMul)
                world -= entity
            }
        }
    }

    private fun detonate(ex: Float, ey: Float, r: Float, blastMul: Float = 1f) {
        Explosion.applyWallDamage(map, ex, ey, config.player, blastMul)
        flow.rebuild(map, playerTileX(), playerTileY(), FlowRebuildSystem.MAX_DIST)
        val maxDmg = config.player.explodeDmg
        mobGrid.forNearby(ex, ey, r + 24f) { mobEntity ->
            val mobT = with(world) { mobEntity[Transform] }
            val ddx = mobT.x - ex; val ddy = mobT.y - ey
            val dist = hypot(ddx, ddy)
            if (dist < r) {
                val fall = 1f - dist / r
                val dmg = maxDmg * fall
                if (dmg > 0f) {
                    val mobH = with(world) { mobEntity[Health] }
                    val mobV = with(world) { mobEntity[Velocity] }
                    val mobA = with(world) { mobEntity[MobAction] }
                    val mobDodge = with(world) { mobEntity[Mob].def.dodge }
                    val nx = if (dist > 0f) ddx / dist else 1f
                    val ny = if (dist > 0f) ddy / dist else 0f
                    MobDamage.hurt(mobH, mobV, mobA, mobDodge, dmg, nx, ny, 280f * fall, rng.nextFloat())
                }
            }
        }
        // v2.80: a blast this size earns its show — three spark rings, a flash, a real kick.
        fx.addShake(0.34f, 13f)
        fx.spawnSparks(ex, ey, 26, Color.valueOf("ffb060"))
        fx.spawnSparks(ex, ey, 16, Color.valueOf("fff2a0"))
        fx.spawnSparks(ex, ey, 10, Color.valueOf("ff6a3a"))
        fx.spawnDeath(ex, ey, Color.valueOf("ffc070"), big = true)
    }

    companion object {
        private const val HOMING_RANGE = 320f // v2.40: seeker rounds track enemies within this radius
    }
}
