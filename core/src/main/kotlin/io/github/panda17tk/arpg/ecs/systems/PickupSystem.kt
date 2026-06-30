package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Buff
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.ecs.components.Mods
import io.github.panda17tk.arpg.ecs.components.Pickup
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Smoke
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.sim.Consequence
import io.github.panda17tk.arpg.sim.WorldState
import kotlin.math.abs

/** Auto-collects pickups when the player walks near; despawns stale ones. Planet materials grant small boons. */
class PickupSystem : IteratingSystem(family { all(Pickup, Transform) }) {
    private val rng: Rng = world.inject()
    private val worldState: WorldState = world.inject()
    private val players by lazy { world.family { all(PlayerTag, Transform, Health, Ammo, Materials, Buff, Mods) } }

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
                        "mat_nature", "mat_magma", "mat_ice", "mat_gas", "mat_dead", "mat_lonely" -> applyMaterial(p, pk.kind)
                        else -> { val a = p[Ammo]; a.set(pk.kind, a.get(pk.kind) + pk.amount) }
                    }
                    world -= entity
                    return@forEach
                }
            }
        }
    }

    /** A planet material's boon — small permanent stat gains; a lonely relic rolls a random one. */
    private fun applyMaterial(p: Entity, kind: String) {
        worldState.society.relicClaimed = true // the planet's relic has been claimed → objective points to the pad
        val mult = Consequence.materialMultiplier(worldState.society) // a slain apex → richer spoils (at a cost)
        val k = if (kind == "mat_lonely") LONELY_ROLL[rng.nextInt(LONELY_ROLL.size)] else kind
        with(world) {
            when (k) {
                "mat_nature" -> { val h = p[Health]; val g = NATURE_HP * mult; h.hpMax += g; h.hp = minOf(h.hpMax, h.hp + g) }
                "mat_magma" -> p[Mods].gunMul += MAGMA_GUN * mult
                "mat_ice" -> p[Mods].fireMul = maxOf(MIN_FIRE, p[Mods].fireMul * ICE_FIRE)
                "mat_gas" -> p[Mods].moveMul += GAS_MOVE * mult
                "mat_dead" -> { val a = p[Ammo]; val g = (DEAD_AMMO * mult).toInt(); for (ak in AMMO_KINDS) a.set(ak, a.get(ak) + g) }
                else -> {}
            }
        }
    }

    companion object {
        private const val LIFE = 14f
        private const val PICK_R = 18f
        private const val NATURE_HP = 8f      // nature core: a little more max HP (and a top-up)
        private const val MAGMA_GUN = 0.1f    // magma core: +10% bullet damage
        private const val ICE_FIRE = 0.9f     // ice core: 10% snappier fire interval
        private const val MIN_FIRE = 0.4f     // ...floored so fire rate can't run away
        private const val GAS_MOVE = 0.08f    // gas core: +8% move speed
        private const val DEAD_AMMO = 12      // dead relic: tops up every ammo pool
        private val AMMO_KINDS = arrayOf("ammo9", "ammo12", "ammoBeam", "ammoNade")
        private val LONELY_ROLL = arrayOf("mat_nature", "mat_magma", "mat_ice", "mat_gas", "mat_dead")
    }
}
