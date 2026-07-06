package io.github.panda17tk.arpg.ecs.systems

import com.badlogic.gdx.graphics.Color
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.combat.MeleeCombo
import io.github.panda17tk.arpg.combat.MeleeResolve
import io.github.panda17tk.arpg.combat.MobDamage
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Bullet
import io.github.panda17tk.arpg.ecs.components.Cooldowns
import io.github.panda17tk.arpg.ecs.components.EBullet
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Gear
import io.github.panda17tk.arpg.ecs.components.Fx
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.MobAction
import io.github.panda17tk.arpg.ecs.components.Mods
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.ecs.world.Pickups
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.map.Tile
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.map.Tiles
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.pathfinding.SpatialGrid
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot

class MeleeSystem(private val mobGrid: SpatialGrid<Entity>) :
    IteratingSystem(family { all(PlayerTag, Transform, Facing, Stamina, Cooldowns, Mods) }) {

    private val input: InputState = world.inject()
    private val map: TileMap = world.inject()
    private val config: GameConfig = world.inject()
    private val rng: Rng = world.inject()
    private val fx: Fx = world.inject()
    private val chipColor = Color.valueOf("8a8076")
    private val comboCool = Color.valueOf("cfe2ff") // v2.80: steps 1-2 — a pale glint
    private val comboHot = Color.valueOf("ffd27a")  // steps 3-4 — warming up
    private val comboGold = Color.valueOf("ffe94a") // step 5 — full rhythm
    private val deflectColor = Color.valueOf("9ec5ff")
    private val ebullets by lazy { world.family { all(EBullet, Transform) } }

    // v2.80 コンボ: the current step and the clock since the last swing (one player, one blade).
    private var comboStep = 0
    private var sinceSwing = 0f
    private var lastCd = 0f

    override fun onTickEntity(entity: Entity) {
        val cd = entity[Cooldowns]
        if (cd.melee > 0f) cd.melee -= deltaTime
        sinceSwing += deltaTime
        // the beat missed: past the (tightening) chain window, the combo lets go
        if (comboStep > 0 && sinceSwing > lastCd + MeleeCombo.chainWindow(comboStep)) comboStep = 0
        if (!input.melee || cd.melee > 0f) return

        val t = entity[Transform]; val f = entity[Facing]; val s = entity[Stamina]; val mods = entity[Mods]
        if (s.overheat) return // no stamina actions while overheated
        // v2.80: on the beat → the combo climbs; off it (or first swing) → step 1.
        comboStep = MeleeCombo.nextStep(comboStep, chained = comboStep > 0)
        lastCd = MeleeCombo.cooldown(comboStep, config.player.meleeCd)
        sinceSwing = 0f
        cd.melee = lastCd
        fx.spawnSlash(t.x, t.y, atan2(f.y, f.x), MeleeCombo.slashScale(comboStep)) // v2.81: the crescent grows
        // the show grows with the rhythm: more sparks each step, gold at the peak, a kick at 3+
        fx.spawnSparks(t.x + f.x * 20f, t.y + f.y * 20f, MeleeCombo.sparks(comboStep),
            if (comboStep >= MeleeCombo.MAX_STEP) comboGold else if (comboStep >= 3) comboHot else comboCool)
        if (comboStep >= 3) fx.addShake(0.10f + 0.03f * comboStep, 2f + comboStep.toFloat())
        val outcome = MeleeResolve.resolve(if (s.max > 0f) s.value / s.max else 1f, config.player)
        s.value = maxOf(0f, s.value - config.player.meleeStaCost) // melee drains stamina

        // Break destructible walls in the front 3x3 (legacy melee.js)
        val ftx = floor((t.x + f.x * Tuning.MELEE_WALL_OFFSET) / Tuning.TILE).toInt()
        val fty = floor((t.y + f.y * Tuning.MELEE_WALL_OFFSET) / Tuning.TILE).toInt()
        for (oy in -1..1) for (ox in -1..1) {
            val tx = ftx + ox; val ty = fty + oy
            if (map.tileAt(tx, ty) == Tile.WALL) {
                val broke = Tiles.damageTile(map, tx, ty, outcome.dmg).broke
                fx.spawnChips((tx + 0.5f) * Tuning.TILE, (ty + 0.5f) * Tuning.TILE, 2, chipColor)
                if (broke) Pickups.dropOnWall(world, rng, (tx + 0.5f) * Tuning.TILE, (ty + 0.5f) * Tuning.TILE)
            }
        }

        // --- Melee vs mob: a 180° base arc at meleeReach, both shaped by the equipped arm (v2.39) ---
        val gearMelee = entity[Gear].loadout // v2.33: the equipped melee arm shapes reach + damage
        val reach = config.player.meleeReach * gearMelee.meleeReachMul * MeleeCombo.reachMul(comboStep) // v2.80
        val arc = (PI.toFloat() * gearMelee.meleeArcMul * MeleeCombo.arcMul(comboStep))
            .coerceAtMost(2f * PI.toFloat()) // 大鉈 swings wider; the combo opens it further
        val faceAng = atan2(f.y, f.x)
        // v2.81: the combo lunges FORWARD — reach stretches dead ahead (cos²-weighted), sides stay honest.
        val fwd = MeleeCombo.forwardMul(comboStep)
        var landedAny = false // v2.89: one pitched hit-sound per swing, not per victim
        mobGrid.forNearby(t.x, t.y, reach * (1f + fwd) + 24f) { mobEntity ->
            val mobT = with(world) { mobEntity[Transform] }
            val mobB = with(world) { mobEntity[Body] }
            val ddx = mobT.x - t.x; val ddy = mobT.y - t.y
            val dist = hypot(ddx, ddy)
            val mobHalf = (mobB.halfW + mobB.halfH) * 0.5f
            run {
                val ang = atan2(ddy, ddx) - faceAng
                val a = abs(((ang + 3f * PI.toFloat()) % (2f * PI.toFloat())) - PI.toFloat())
                val ahead = kotlin.math.cos(a).coerceAtLeast(0f)
                val effReach = reach * (1f + fwd * ahead * ahead)
                if (dist < effReach + mobHalf && a <= arc / 2f) {
                    val mobH = with(world) { mobEntity[Health] }
                    val mobV = with(world) { mobEntity[Velocity] }
                    val mobA = with(world) { mobEntity[MobAction] }
                    val mobDodge = with(world) { mobEntity[Mob].def.dodge }
                    val nx = if (dist > 0f) ddx / dist else 1f
                    val ny = if (dist > 0f) ddy / dist else 0f
                    val hpBefore = mobH.hp
                    val dealt = outcome.dmg * mods.meleeMul * gearMelee.meleeDmgMul * MeleeCombo.dmgMul(comboStep) // v2.80
                    val landed = MobDamage.hurt(mobH, mobV, mobA, mobDodge, dealt, nx, ny, 450f * gearMelee.meleeKbMul, rng.nextFloat())
                    if (landed) { // v2.85: the hold + the number sell the swing
                        landedAny = true
                        fx.hitstop(MeleeCombo.hitstop(comboStep))
                        fx.spawnPop(
                            mobT.x, mobT.y - mobB.halfH - 6f, dealt.toInt(),
                            if (comboStep >= MeleeCombo.MAX_STEP) comboGold else if (comboStep >= 3) comboHot else comboCool,
                            1f + 0.07f * (comboStep - 1),
                        )
                    }
                    // v2.42: a leeching arm drinks back a fraction of the damage that actually landed.
                    if (gearMelee.meleeLifesteal > 0f && mobH.hp < hpBefore) {
                        val ph = entity[Health]
                        ph.hp = (ph.hp + (hpBefore - mobH.hp) * gearMelee.meleeLifesteal).coerceAtMost(ph.hpMax)
                    }
                }
            }
        }
        if (landedAny) fx.requestSfx("melee_hit", 1f + 0.09f * (comboStep - 1)) // v2.89: the rhythm climbs
        // v2.39: a resonant blade throws the swing as a wave — a short-lived 3-shard fan that
        // carries 60% of the melee damage (and chips blocks at half strength).
        if (gearMelee.meleeWave) {
            val waveDmg = outcome.dmg * mods.meleeMul * gearMelee.meleeDmgMul * WAVE_DMG_FRAC
            for (off in floatArrayOf(-WAVE_FAN, 0f, WAVE_FAN)) {
                val a = faceAng + off
                world.entity {
                    it += Transform(x = t.x + kotlin.math.cos(a) * Tuning.MUZZLE_OFFSET, y = t.y + kotlin.math.sin(a) * Tuning.MUZZLE_OFFSET)
                    it += Bullet(kotlin.math.cos(a) * WAVE_SPEED, kotlin.math.sin(a) * WAVE_SPEED, WAVE_LIFE, waveDmg, wallMul = 0.5f)
                }
            }
        }

        // deflect enemy bullets caught in the swing arc
        ebullets.forEach { be ->
            val bt = with(world) { be[Transform] }
            val bdx = bt.x - t.x; val bdy = bt.y - t.y
            val bdist = hypot(bdx, bdy)
            if (bdist < reach + 8f) {
                val bang = atan2(bdy, bdx) - faceAng
                val ba = abs(((bang + 3f * PI.toFloat()) % (2f * PI.toFloat())) - PI.toFloat())
                if (ba <= arc / 2f) { world -= be; fx.spawnSparks(bt.x, bt.y, 6, deflectColor) }
            }
        }
    }

    companion object {
        private const val WAVE_DMG_FRAC = 0.6f // a flying slash carries this fraction of the swing's damage
        private const val WAVE_SPEED = 320f    // slower than bullets — a visible wave, not a shot
        private const val WAVE_LIFE = 0.35f    // short reach (~110px), melee-flavoured
        private const val WAVE_FAN = 0.32f     // radians between the three shards
    }
}
