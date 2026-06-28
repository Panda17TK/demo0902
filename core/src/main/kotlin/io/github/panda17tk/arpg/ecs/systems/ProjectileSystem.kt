package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.combat.Ballistics
import io.github.panda17tk.arpg.combat.Explosion
import io.github.panda17tk.arpg.ecs.components.Bullet
import io.github.panda17tk.arpg.ecs.components.Grenade
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.map.Tiles
import io.github.panda17tk.arpg.pathfinding.FlowField
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.floor

/** Advances player bullets and grenades; breaks walls; explodes grenades. Mob damage: Phase 5. */
class ProjectileSystem : IteratingSystem(family { any(Bullet, Grenade) }) {
    private val map: TileMap = world.inject()
    private val flow: FlowField = world.inject()

    private var pPlayerX = 0
    private var pPlayerY = 0

    private fun playerTileX(): Int = pPlayerX
    private fun playerTileY(): Int = pPlayerY

    override fun onTick() {
        // cache the player tile once per frame so wall-break rebuilds use it
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
            val wall = step.wallTile
            if (wall != null) {
                val broke = Tiles.damageTile(map, wall.first, wall.second, b.dmg).broke
                if (broke) flow.rebuild(map, playerTileX(), playerTileY())
                world -= entity
                return
            }
            if (step.expired) world -= entity
        } else {
            val g = entity[Grenade]
            t.x += g.vx * deltaTime; t.y += g.vy * deltaTime; g.fuse -= deltaTime
            val tx = floor(t.x / Tuning.TILE).toInt(); val ty = floor(t.y / Tuning.TILE).toInt()
            if (map.solidAt(tx, ty) || g.fuse <= 0f) {
                Explosion.applyWallDamage(map, t.x, t.y) // player self-damage + mob damage: Phase 5
                flow.rebuild(map, playerTileX(), playerTileY())
                world -= entity
            }
        }
    }
}
