package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.combat.BeamRay
import io.github.panda17tk.arpg.combat.Firing
import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.Bullet
import io.github.panda17tk.arpg.ecs.components.Cooldowns
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Grenade
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class FireSystem : IteratingSystem(family { all(PlayerTag, Transform, Facing, Arsenal, Ammo, Cooldowns) }) {
    private val input: InputState = world.inject()
    private val map: TileMap = world.inject()
    private val rng: Rng = world.inject()

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
                BeamRay.cast(map, t.x, t.y, dirX, dirY, 700f) // mob hits land here in Phase 5
            }
            "grenade" -> {
                if (w.mag <= 0) return
                w.mag--; cd.shoot = def.fireRate
                world.entity {
                    it += Transform(x = t.x + dirX * Tuning.MUZZLE_OFFSET, y = t.y + dirY * Tuning.MUZZLE_OFFSET)
                    it += Grenade(dirX * Tuning.GRENADE_SPEED, dirY * Tuning.GRENADE_SPEED, Tuning.GRENADE_FUSE)
                }
            }
            else -> {
                if (w.mag <= 0) return
                w.mag--; cd.shoot = def.fireRate
                val angles = Firing.bulletAngles(aim, def.spread, def.pellets, rng)
                for (a in angles) {
                    val vx = cos(a) * Tuning.BULLET_SPEED; val vy = sin(a) * Tuning.BULLET_SPEED
                    world.entity {
                        it += Transform(x = t.x + cos(a) * Tuning.MUZZLE_OFFSET, y = t.y + sin(a) * Tuning.MUZZLE_OFFSET)
                        it += Bullet(vx, vy, Tuning.BULLET_LIFE, def.dmg)
                    }
                }
            }
        }
    }
}
