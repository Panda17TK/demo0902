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
import kotlin.math.floor
import kotlin.math.hypot

/** Advances player bullets and grenades; breaks walls; explodes grenades; damages mobs (with dodge). */
class ProjectileSystem(private val mobGrid: SpatialGrid<Entity>) :
    IteratingSystem(family { any(Bullet, Grenade) }) {

    private val map: TileMap = world.inject()
    private val flow: FlowField = world.inject()
    private val config: GameConfig = world.inject()
    private val rng: Rng = world.inject()
    private val fx: Fx = world.inject()

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
                if (Tiles.damageTile(map, wall.first, wall.second, b.dmg).broke) {
                    flow.rebuild(map, playerTileX(), playerTileY())
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
                detonate(t.x, t.y, if (prox) Tuning.TILE * 2f else config.player.explodeRadius)
                world -= entity
            }
        }
    }

    private fun detonate(ex: Float, ey: Float, r: Float) {
        Explosion.applyWallDamage(map, ex, ey, config.player)
        flow.rebuild(map, playerTileX(), playerTileY())
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
        fx.addShake(0.22f, 8f)
        fx.spawnSparks(ex, ey, 14, Color.valueOf("ffb060"))
    }
}
