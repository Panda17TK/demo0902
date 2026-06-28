package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.combat.BeamRay
import io.github.panda17tk.arpg.combat.Firing
import io.github.panda17tk.arpg.combat.MobDamage
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Bullet
import io.github.panda17tk.arpg.ecs.components.Cooldowns
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Grenade
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.pathfinding.SpatialGrid
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class FireSystem(private val mobGrid: SpatialGrid<Entity>) :
    IteratingSystem(family { all(PlayerTag, Transform, Facing, Arsenal, Ammo, Cooldowns) }) {

    private val input: InputState = world.inject()
    private val map: TileMap = world.inject()
    private val rng: Rng = world.inject()
    private val config: GameConfig = world.inject()

    override fun onTickEntity(entity: Entity) {
        val cd = entity[Cooldowns]
        if (cd.shoot > 0f) cd.shoot -= deltaTime
        if (!input.fire || cd.shoot > 0f) return

        val t = entity[Transform]; val f = entity[Facing]
        val arsenal = entity[Arsenal]; val ammo = entity[Ammo]
        val w = arsenal.current; val def = w.def
        val aim = atan2(f.y, f.x)
        val dirX = cos(aim); val dirY = sin(aim)

        when (def.id) {
            "beam" -> {
                if (ammo.ammoBeam <= 0) return
                ammo.ammoBeam--
                cd.shoot = def.fireRate
                val hit = BeamRay.cast(map, t.x, t.y, dirX, dirY, 700f)

                // --- Beam vs mob: query mobs near the ray, check projection + perpendicular distance ---
                // Port of legacy combat.js beam mob loop (lines ~185-205)
                mobGrid.forNearby(t.x, t.y, hit.reach + 32f) { mobEntity ->
                    val mobT = with(world) { mobEntity[Transform] }
                    val mobB = with(world) { mobEntity[Body] }
                    val rx = mobT.x - t.x; val ry = mobT.y - t.y
                    // Projection along ray direction
                    val s = rx * dirX + ry * dirY
                    val mobHalf = (mobB.halfW + mobB.halfH) * 0.5f
                    if (s < -mobHalf || s > hit.reach + mobHalf) return@forNearby
                    // Perpendicular distance from ray line
                    val perp = abs(rx * dirY - ry * dirX)
                    if (perp > mobHalf) return@forNearby
                    // Single hit in 5a (multi-hit is 5b)
                    val mobH = with(world) { mobEntity[Health] }
                    val mobV = with(world) { mobEntity[Velocity] }
                    MobDamage.hurt(mobH, mobV, def.dmg, 0f, 0f, 0f)
                }
            }
            "grenade" -> {
                if (w.mag <= 0) return
                w.mag--; cd.shoot = def.fireRate
                world.entity {
                    it += Transform(x = t.x + dirX * Tuning.MUZZLE_OFFSET, y = t.y + dirY * Tuning.MUZZLE_OFFSET)
                    it += Grenade(dirX * config.player.grenadeSpeed, dirY * config.player.grenadeSpeed, config.player.grenadeFuse)
                }
            }
            else -> {
                if (w.mag <= 0) return
                w.mag--; cd.shoot = def.fireRate
                val angles = Firing.bulletAngles(aim, def.spread, def.pellets, rng)
                for (a in angles) {
                    val vx = cos(a) * config.player.bulletSpeed; val vy = sin(a) * config.player.bulletSpeed
                    world.entity {
                        it += Transform(x = t.x + cos(a) * Tuning.MUZZLE_OFFSET, y = t.y + sin(a) * Tuning.MUZZLE_OFFSET)
                        it += Bullet(vx, vy, config.player.bulletLife, def.dmg)
                    }
                }
            }
        }
    }
}
