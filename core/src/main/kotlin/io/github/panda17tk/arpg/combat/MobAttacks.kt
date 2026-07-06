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
 * Normal types: melee/shot/lunge/charge_melee/blink + v2.41's spiral/spray/twin_shot/shockwave.
 * Boss types: burst/nova/summon/slam/charge/homing/heal/enrage/mine/barrage/guard.
 * `charge_melee`/`blink` set MobAction state
 * (finished by MobActionSystem); enrage/guard set MobAction timers (read by AISystem/MobDamage).
 */
object MobAttacks {
    private val TWO_PI = (Math.PI * 2.0).toFloat()
    private const val CUTOFF_LEAD = 130f // v2.94: how far ahead of the runner the echo lands

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
        // --- v2.41 attack types ---
        // spiral: two interleaved rings (fast + slow) that bloom outward as a slow flower.
        "spiral" -> { spiral(world, mobT, spec.count, spec.speed, spec.dmg, spec.life); true }
        // spray: undisciplined suppression fire — random angles in the fan, jittered speeds.
        "spray" -> if (!see) false else {
            spray(world, mobT, atan2(toPy, toPx), spec.count, spec.spread, spec.speed, spec.dmg, spec.life, rng); true
        }
        // twin_shot: two parallel rounds, one off each shoulder.
        "twin_shot" -> if (!see) false else { twin(world, mobT, atan2(toPy, toPx), spec.speed, spec.dmg, spec.life); true }
        // shockwave: a pressure ring — the shove lands even mid-iframe; only naked hits take the (small) damage.
        "shockwave" -> if (dist > spec.range) false else {
            playerV.vx += toPx * spec.power; playerV.vy += toPy * spec.power
            if (playerH.iTime <= 0f) { playerH.hp -= spec.dmg * dmgTakenMul; playerH.iTime = iFrameMelee }
            true
        }
        // --- v2.94 固有ボス技 ---
        // ring_gap (錆の巨人): a full ring with ONE deterministic gap — read it, dash through it.
        "ring_gap" -> { ringGap(world, mobT, spec.count, spec.spread, spec.speed, spec.dmg, spec.life, rng); true }
        // cutoff_volley (合唱ノード): the echo lands ahead of the fleeing player and sings back —
        // running in straight lines is punished; a standing player just gets an aimed shot.
        "cutoff_volley" -> if (!see) false else {
            cutoff(world, mobT, dist, toPx, toPy, playerV, spec.count, spec.speed, spec.dmg, spec.life); true
        }
        // page_wall (書庫長): a written line — bullets abreast, advancing as a wall with dodgeable edges.
        "page_wall" -> if (!see) false else {
            pageWall(world, mobT, atan2(toPy, toPx), spec.count, spec.spread, spec.speed, spec.dmg, spec.life); true
        }
        else -> false
    }

    /** v2.94: a radial ring with a [gapDeg]-wide silence at a deterministic angle. */
    private fun ringGap(world: World, mobT: Transform, count: Int, gapDeg: Float, speed: Float, dmg: Float, life: Float, rng: Rng) {
        val gapAt = rng.nextFloat() * TWO_PI
        val gapHalf = gapDeg * PI.toFloat() / 180f / 2f
        for (i in 0 until count) {
            val a = TWO_PI * i / count
            val delta = kotlin.math.abs(((a - gapAt + 3f * PI.toFloat()) % TWO_PI) - PI.toFloat())
            if (delta < gapHalf) continue // the gap the keeper reads and slips through
            world.entity {
                it += Transform(x = mobT.x, y = mobT.y, prevX = mobT.x, prevY = mobT.y)
                it += EBullet(cos(a) * speed, sin(a) * speed, life, dmg)
            }
        }
    }

    /** v2.94: shots seeded AHEAD of the player's motion, singing back at them. */
    private fun cutoff(
        world: World, mobT: Transform, dist: Float, toPx: Float, toPy: Float, playerV: Velocity,
        count: Int, speed: Float, dmg: Float, life: Float,
    ) {
        val px = mobT.x + toPx * dist; val py = mobT.y + toPy * dist
        val vLen = kotlin.math.hypot(playerV.vx + playerV.driftX, playerV.vy + playerV.driftY)
        if (vLen < 30f) { // a standing listener just hears one aimed note
            fire(world, mobT, atan2(toPy, toPx), speed, dmg, life)
            return
        }
        val ux = (playerV.vx + playerV.driftX) / vLen; val uy = (playerV.vy + playerV.driftY) / vLen
        val perpX = -uy; val perpY = ux
        for (i in 0 until count) {
            val off = (i - (count - 1) / 2f) * 26f
            val sx = px + ux * CUTOFF_LEAD + perpX * off
            val sy = py + uy * CUTOFF_LEAD + perpY * off
            world.entity {
                it += Transform(x = sx, y = sy, prevX = sx, prevY = sy)
                it += EBullet(-ux * speed, -uy * speed, life, dmg)
            }
        }
    }

    /** v2.94: [count] bullets abreast (spread px apart), advancing as one written line. */
    private fun pageWall(world: World, mobT: Transform, ang: Float, count: Int, spreadPx: Float, speed: Float, dmg: Float, life: Float) {
        val dx = cos(ang); val dy = sin(ang)
        val perpX = -dy; val perpY = dx
        for (i in 0 until count) {
            val off = (i - (count - 1) / 2f) * spreadPx
            val sx = mobT.x + perpX * off; val sy = mobT.y + perpY * off
            world.entity {
                it += Transform(x = sx, y = sy, prevX = sx, prevY = sy)
                it += EBullet(dx * speed, dy * speed, life, dmg)
            }
        }
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

    /** v2.41 spiral: a full ring with alternating fast/slow bullets — two rings that drift apart. */
    private fun spiral(world: World, mobT: Transform, count: Int, speed: Float, dmg: Float, life: Float) {
        val n = if (count < 1) 1 else count
        for (i in 0 until n) {
            val sp = if (i % 2 == 0) speed else speed * 0.55f
            fire(world, mobT, (i.toFloat() / n) * 2f * PI.toFloat(), sp, dmg, life)
        }
    }

    /** v2.41 spray: [count] rounds at random angles inside the fan, speeds jittered ±20%. */
    @Suppress("LongParameterList")
    private fun spray(world: World, mobT: Transform, base: Float, count: Int, spreadDeg: Float, speed: Float, dmg: Float, life: Float, rng: Rng) {
        val n = if (count < 1) 1 else count
        val spread = spreadDeg * PI.toFloat() / 180f
        repeat(n) {
            val a = base + (rng.nextFloat() - 0.5f) * spread
            fire(world, mobT, a, speed * (0.8f + rng.nextFloat() * 0.4f), dmg, life)
        }
    }

    /** v2.41 twin_shot: two parallel rounds offset one half-body to each side of the aim line. */
    private fun twin(world: World, mobT: Transform, ang: Float, speed: Float, dmg: Float, life: Float) {
        val px = -sin(ang); val py = cos(ang) // perpendicular to the aim
        for (sgn in intArrayOf(-1, 1)) {
            val ox = mobT.x + px * sgn * TWIN_GAP; val oy = mobT.y + py * sgn * TWIN_GAP
            world.entity {
                it += Transform(x = ox, y = oy, prevX = ox, prevY = oy)
                it += EBullet(cos(ang) * speed, sin(ang) * speed, life, dmg)
            }
        }
    }

    private const val TWIN_GAP = 9f // half the shoulder width between the twin rounds

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
