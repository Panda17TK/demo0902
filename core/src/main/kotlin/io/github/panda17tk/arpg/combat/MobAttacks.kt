package io.github.panda17tk.arpg.combat

import com.github.quillraven.fleks.World
import io.github.panda17tk.arpg.config.AttackSpec
import io.github.panda17tk.arpg.config.EnemyDef
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.EBullet
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.MobAction
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.ecs.world.MobFactory
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Enemy attack handlers, ported from legacy systems/attacks.js REGISTRY.
 * Returns true if the attack executed (so the caller starts its cooldown).
 * Normal types: melee/shot/lunge/charge_melee/blink. Boss types: burst/nova/summon/slam/
 * charge/homing/heal/enrage/mine/barrage/guard. `charge_melee`/`blink` set MobAction state
 * (finished by MobActionSystem); enrage/guard set MobAction timers (read by AISystem/MobDamage).
 */
object MobAttacks {
    @Suppress("LongParameterList")
    fun tryAttack(
        world: World,
        spec: AttackSpec,
        def: EnemyDef,
        mobT: Transform,
        mobFacing: Facing,
        mobVel: Velocity,
        action: MobAction,
        mobH: Health,
        playerH: Health,
        playerV: Velocity,
        dist: Float,
        toPx: Float,
        toPy: Float,
        see: Boolean,
        iFrameMelee: Float,
        config: GameConfig,
        waveNum: Int,
        rng: Rng,
        dmgTakenMul: Float = 1f, // v2.33: armor — scales damage the player takes
    ): Boolean = when (spec.type) {
        "melee" -> when {
            dist >= spec.range -> false
            spec.arc < 360f && acos((mobFacing.x * toPx + mobFacing.y * toPy).coerceIn(-1f, 1f)) > (spec.arc * PI.toFloat() / 180f) / 2f -> false
            else -> { hitPlayer(playerH, playerV, spec.dmg * dmgTakenMul, toPx, toPy, def.contactKB, iFrameMelee); true }
        }
        "shot" -> if (!see) false else { fire(world, mobT, atan2(toPy, toPx), spec.speed, spec.dmg, spec.life); true }
        "lunge" -> if (!see || dist > spec.range) false else {
            mobVel.vx += toPx * spec.power; mobVel.vy += toPy * spec.power; true
        }
        "charge_melee" -> if (action.charging || !see || dist > spec.range) false else {
            action.chargeT = spec.windup; action.chargeDx = toPx; action.chargeDy = toPy; true
        }
        "blink" -> if (action.blinkChargeT > 0f || action.blinkT > 0f || !see || dist < spec.minDist) false else {
            val want = (dist - spec.standoff).coerceIn(0f, spec.maxTiles * Tuning.TILE)
            action.blinkChargeT = 0.25f; action.blinkTotal = spec.dur // visible charge windup before teleport
            action.blinkDx = toPx * want; action.blinkDy = toPy * want; true
        }
        // --- Phase 6c boss attacks ---
        "burst", "barrage" -> if (!see) false else {
            fan(world, mobT, atan2(toPy, toPx), spec.count, spec.spread, spec.speed, spec.dmg, spec.life); true
        }
        "nova" -> { radial(world, mobT, spec.count, spec.speed, spec.dmg, spec.life); true }
        "homing" -> if (!see) false else {
            val ang = atan2(toPy, toPx)
            world.entity {
                it += Transform(x = mobT.x, y = mobT.y, prevX = mobT.x, prevY = mobT.y)
                it += EBullet(cos(ang) * spec.speed, sin(ang) * spec.speed, spec.life, spec.dmg, homing = spec.turn)
            }
            true
        }
        "mine" -> {
            world.entity {
                it += Transform(x = mobT.x, y = mobT.y, prevX = mobT.x, prevY = mobT.y)
                it += EBullet(0f, 0f, spec.life, spec.dmg, mine = true)
            }
            true
        }
        "slam" -> if (dist > spec.range) false else { hitPlayer(playerH, playerV, spec.dmg * dmgTakenMul, toPx, toPy, spec.power, iFrameMelee); true }
        "charge" -> if (!see || dist > spec.range || dist < 40f) false else {
            mobVel.vx += toPx * spec.power; mobVel.vy += toPy * spec.power; true
        }
        "heal" -> if (mobH.hp >= mobH.hpMax) false else { mobH.hp = minOf(mobH.hpMax, mobH.hp + spec.amount); true }
        "enrage" -> { action.enrageT = spec.duration; action.enrageMul = spec.mul; true }
        "guard" -> { action.guardT = spec.duration; action.guardMul = spec.mul; true }
        "summon" -> { summon(world, config, rng, spec.minion, spec.count, mobT.x, mobT.y, waveNum); true }
        else -> false
    }

    private fun hitPlayer(playerH: Health, playerV: Velocity, dmg: Float, toPx: Float, toPy: Float, kb: Float, iFrameMelee: Float) {
        if (playerH.iTime > 0f) return
        playerH.hp -= dmg
        playerH.iTime = iFrameMelee
        playerV.vx += toPx * kb
        playerV.vy += toPy * kb
    }

    /** One enemy bullet aimed at [ang] (legacy fireBullet). */
    private fun fire(world: World, mobT: Transform, ang: Float, speed: Float, dmg: Float, life: Float) {
        world.entity {
            it += Transform(x = mobT.x, y = mobT.y, prevX = mobT.x, prevY = mobT.y)
            it += EBullet(cos(ang) * speed, sin(ang) * speed, life, dmg)
        }
    }

    /** Fan of [count] bullets spanning [spreadDeg] degrees, centered on [base] (legacy burst/barrage). */
    private fun fan(world: World, mobT: Transform, base: Float, count: Int, spreadDeg: Float, speed: Float, dmg: Float, life: Float) {
        val n = if (count < 1) 1 else count
        val spread = spreadDeg * PI.toFloat() / 180f
        for (i in 0 until n) {
            val t = if (n > 1) (i.toFloat() / (n - 1) - 0.5f) else 0f
            fire(world, mobT, base + t * spread, speed, dmg, life)
        }
    }

    /** [count] bullets evenly around the full circle (legacy nova). */
    private fun radial(world: World, mobT: Transform, count: Int, speed: Float, dmg: Float, life: Float) {
        val n = if (count < 1) 1 else count
        for (i in 0 until n) fire(world, mobT, (i.toFloat() / n) * 2f * PI.toFloat(), speed, dmg, life)
    }

    /** Spawn [count] minions of [minion] near (x,y), scaled to [waveNum] (legacy summon). */
    private fun summon(world: World, config: GameConfig, rng: Rng, minion: String, count: Int, x: Float, y: Float, waveNum: Int) {
        val def = config.enemies[minion] ?: return
        val n = if (count < 1) 1 else count
        repeat(n) {
            val ang = rng.nextFloat() * 2f * PI.toFloat()
            val r = 28f + rng.nextFloat() * 16f
            MobFactory.spawn(world, def, x + cos(ang) * r, y + sin(ang) * r, waveNum, config.waves.hpScalePerWave, config.waves.speedScalePerWave)
        }
    }
}
