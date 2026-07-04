package io.github.panda17tk.arpg.ecs.systems

import com.badlogic.gdx.graphics.Color
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
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
    private val deflectColor = Color.valueOf("9ec5ff")
    private val ebullets by lazy { world.family { all(EBullet, Transform) } }

    override fun onTickEntity(entity: Entity) {
        val cd = entity[Cooldowns]
        if (cd.melee > 0f) cd.melee -= deltaTime
        if (!input.melee || cd.melee > 0f) return

        val t = entity[Transform]; val f = entity[Facing]; val s = entity[Stamina]; val mods = entity[Mods]
        if (s.overheat) return // no stamina actions while overheated
        cd.melee = config.player.meleeCd
        fx.spawnSlash(t.x, t.y, atan2(f.y, f.x))
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
        val reach = config.player.meleeReach * gearMelee.meleeReachMul
        val arc = (PI.toFloat() * gearMelee.meleeArcMul).coerceAtMost(2f * PI.toFloat()) // 大鉈 swings wider
        val faceAng = atan2(f.y, f.x)
        mobGrid.forNearby(t.x, t.y, reach + 24f) { mobEntity ->
            val mobT = with(world) { mobEntity[Transform] }
            val mobB = with(world) { mobEntity[Body] }
            val ddx = mobT.x - t.x; val ddy = mobT.y - t.y
            val dist = hypot(ddx, ddy)
            val mobHalf = (mobB.halfW + mobB.halfH) * 0.5f
            if (dist < reach + mobHalf) {
                val ang = atan2(ddy, ddx) - faceAng
                val a = abs(((ang + 3f * PI.toFloat()) % (2f * PI.toFloat())) - PI.toFloat())
                if (a <= arc / 2f) {
                    val mobH = with(world) { mobEntity[Health] }
                    val mobV = with(world) { mobEntity[Velocity] }
                    val mobA = with(world) { mobEntity[MobAction] }
                    val mobDodge = with(world) { mobEntity[Mob].def.dodge }
                    val nx = if (dist > 0f) ddx / dist else 1f
                    val ny = if (dist > 0f) ddy / dist else 0f
                    val hpBefore = mobH.hp
                    val dealt = outcome.dmg * mods.meleeMul * gearMelee.meleeDmgMul
                    MobDamage.hurt(mobH, mobV, mobA, mobDodge, dealt, nx, ny, 450f * gearMelee.meleeKbMul, rng.nextFloat())
                    // v2.42: a leeching arm drinks back a fraction of the damage that actually landed.
                    if (gearMelee.meleeLifesteal > 0f && mobH.hp < hpBefore) {
                        val ph = entity[Health]
                        ph.hp = (ph.hp + (hpBefore - mobH.hp) * gearMelee.meleeLifesteal).coerceAtMost(ph.hpMax)
                    }
                }
            }
        }
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
