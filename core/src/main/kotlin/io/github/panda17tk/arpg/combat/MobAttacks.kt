package io.github.panda17tk.arpg.combat

import com.github.quillraven.fleks.World
import io.github.panda17tk.arpg.config.AttackSpec
import io.github.panda17tk.arpg.config.EnemyDef
import io.github.panda17tk.arpg.ecs.components.EBullet
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.MobAction
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.PI
import kotlin.math.acos

/**
 * Enemy attack handlers, ported from legacy systems/attacks.js REGISTRY.
 * Returns true if the attack executed (so the caller starts its cooldown).
 * `shot` spawns an EBullet entity; `charge_melee`/`blink` set MobAction state
 * (finished by MobActionSystem); `melee` damages the player directly.
 * Boss-only types (burst/nova/summon/slam/charge/homing/heal/enrage/mine/barrage/guard) → Phase 5c (return false).
 */
object MobAttacks {
    fun tryAttack(
        world: World,
        spec: AttackSpec,
        def: EnemyDef,
        mobT: Transform,
        mobFacing: Facing,
        mobVel: Velocity,
        action: MobAction,
        playerH: Health,
        playerV: Velocity,
        dist: Float,
        toPx: Float,
        toPy: Float,
        see: Boolean,
        iFrameMelee: Float,
    ): Boolean = when (spec.type) {
        "melee" -> when {
            dist >= spec.range -> false
            spec.arc < 360f && acos((mobFacing.x * toPx + mobFacing.y * toPy).coerceIn(-1f, 1f)) > (spec.arc * PI.toFloat() / 180f) / 2f -> false
            else -> { hitPlayer(playerH, playerV, spec.dmg, toPx, toPy, def.contactKB, iFrameMelee); true }
        }
        "shot" -> if (!see) false else {
            world.entity {
                it += Transform(x = mobT.x, y = mobT.y, prevX = mobT.x, prevY = mobT.y)
                it += EBullet(toPx * spec.speed, toPy * spec.speed, spec.life, spec.dmg)
            }
            true
        }
        "lunge" -> if (!see || dist > spec.range) false else {
            mobVel.vx += toPx * spec.power
            mobVel.vy += toPy * spec.power
            true
        }
        "charge_melee" -> if (action.charging || !see || dist > spec.range) false else {
            action.chargeT = spec.windup
            action.chargeDx = toPx
            action.chargeDy = toPy
            true
        }
        "blink" -> if (!see || dist < spec.minDist) false else {
            val want = (dist - spec.standoff).coerceIn(0f, spec.maxTiles * Tuning.TILE)
            action.blinkT = spec.dur
            action.blinkTotal = spec.dur
            action.blinkDx = toPx * want
            action.blinkDy = toPy * want
            true
        }
        else -> false
    }

    private fun hitPlayer(playerH: Health, playerV: Velocity, dmg: Float, toPx: Float, toPy: Float, kb: Float, iFrameMelee: Float) {
        if (playerH.iTime > 0f) return
        playerH.hp -= dmg
        playerH.iTime = iFrameMelee
        playerV.vx += toPx * kb
        playerV.vy += toPy * kb
    }
}
