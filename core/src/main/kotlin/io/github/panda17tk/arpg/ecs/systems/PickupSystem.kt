package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Buff
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.ecs.components.Pickup
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Smoke
import io.github.panda17tk.arpg.ecs.components.Transform
import kotlin.math.abs

/** Auto-collects pickups when the player walks near; despawns stale ones. */
class PickupSystem : IteratingSystem(family { all(Pickup, Transform) }) {
    private val players by lazy { world.family { all(PlayerTag, Transform, Health, Ammo, Materials, Buff) } }

    override fun onTickEntity(entity: Entity) {
        val t = entity[Transform]; val pk = entity[Pickup]
        pk.t += deltaTime
        if (pk.t > LIFE) { world -= entity; return }
        with(world) {
            players.forEach { p ->
                val pt = p[Transform]
                if (abs(pt.x - t.x) < PICK_R && abs(pt.y - t.y) < PICK_R) {
                    when (pk.kind) {
                        "blocks" -> p[Materials].blocks += pk.amount
                        "med" -> { val h = p[Health]; h.hp = minOf(h.hpMax, h.hp + pk.amount) }
                        "staminaInf" -> p[Buff].staminaInfT = 6f
                        "dashUp" -> p[Buff].dashUpT = 8f
                        "smoke" -> {
                            val pp = p[Transform]
                            world.entity { it += Transform(x = pp.x, y = pp.y, prevX = pp.x, prevY = pp.y); it += Smoke(radius = 80f, life = 5f) }
                        }
                        else -> { val a = p[Ammo]; a.set(pk.kind, a.get(pk.kind) + pk.amount) }
                    }
                    world -= entity
                    return@forEach
                }
            }
        }
    }

    companion object {
        private const val LIFE = 14f
        private const val PICK_R = 18f
    }
}
