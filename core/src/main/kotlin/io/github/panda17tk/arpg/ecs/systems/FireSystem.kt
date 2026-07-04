package io.github.panda17tk.arpg.ecs.systems

import com.badlogic.gdx.graphics.Color
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.combat.BeamRay
import io.github.panda17tk.arpg.combat.Explosion
import io.github.panda17tk.arpg.combat.Firing
import io.github.panda17tk.arpg.combat.MobDamage
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Bullet
import io.github.panda17tk.arpg.ecs.components.Cooldowns
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Gear
import io.github.panda17tk.arpg.ecs.components.Fx
import io.github.panda17tk.arpg.ecs.components.Grenade
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.MobAction
import io.github.panda17tk.arpg.ecs.components.Mods
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.pathfinding.FlowField
import io.github.panda17tk.arpg.pathfinding.SpatialGrid
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin

class FireSystem(private val mobGrid: SpatialGrid<Entity>) :
    IteratingSystem(family { all(PlayerTag, Transform, Facing, Arsenal, Ammo, Cooldowns, Mods) }) {

    private val input: InputState = world.inject()
    private val map: TileMap = world.inject()
    private val rng: Rng = world.inject()
    private val config: GameConfig = world.inject()
    private val fx: Fx = world.inject()
    private val flow: FlowField = world.inject()

    override fun onTickEntity(entity: Entity) {
        val cd = entity[Cooldowns]
        if (cd.shoot > 0f) cd.shoot -= deltaTime

        val arsenal = entity[Arsenal]
        val w = arsenal.current; val def = w.def
        // v2.39 beam charge: while the beam is drawn and being aimed (right stick / K held), the
        // emitter charges 0..1; the release shot gets thicker + harder the longer it charged.
        if (def.id == "beam") {
            if ((input.aiming || input.fire) && cd.shoot <= 0f && w.reloadT <= 0f) {
                cd.beamCharge = (cd.beamCharge + deltaTime / BEAM_CHARGE_TIME).coerceAtMost(1f)
            }
        } else if (cd.beamCharge > 0f) cd.beamCharge = 0f // switching away drops the charge
        // Manual-fire weapons (beam/grenade) shoot on the release edge; everything else fires while held.
        val triggered = if (def.manualFire) input.fireRelease else input.fire
        if (!triggered || cd.shoot > 0f) return
        if (w.reloadT > 0f) return // can't fire mid-reload

        val t = entity[Transform]; val f = entity[Facing]
        val ammo = entity[Ammo]; val mods = entity[Mods]
        val gearGun = entity[Gear].loadout.gunMul // v2.33: scopes etc. sweeten the guns
        // v2.37 weapon grades: handling tweaks apply only while the equipped gun IS the active weapon.
        val gradeItem = entity[Gear].loadout.ranged?.takeIf { it.weaponType == def.id }
        val gradeFireRate = gradeItem?.fireRateMul ?: 1f
        val gradeBlast = gradeItem?.blastMul ?: 1f
        val gradeWall = gradeItem?.wallDmgMul ?: 1f
        val aim = atan2(f.y, f.x)
        val dirX = cos(aim); val dirY = sin(aim)

        when (def.id) {
            "beam" -> {
                if (!def.infiniteAmmo) { // v2.37: the beam runs on the ship's reactor — no ammo
                    if (ammo.ammoBeam <= 0) return
                    ammo.ammoBeam--
                }
                cd.shoot = def.fireRate * mods.fireMul * gradeFireRate
                // v2.39: cash in the charge — damage up to 2×, the ray visibly fattens, and the
                // pierce corridor widens with it. (The beam always pierces every mob on the ray.)
                val charge = cd.beamCharge
                cd.beamCharge = 0f
                val chargeDmg = 1f + charge * BEAM_CHARGE_DMG
                val beamHalfW = BEAM_W_MIN + charge * (BEAM_W_MAX - BEAM_W_MIN)
                val hit = BeamRay.cast(map, t.x, t.y, dirX, dirY, BEAM_RANGE)
                fx.spawnBeam(t.x, t.y, t.x + dirX * hit.reach, t.y + dirY * hit.reach, beamHalfW)

                // --- Beam vs mob: query mobs near the ray, check projection + perpendicular distance ---
                // The beam PIERCES enemies: every mob along the ray (up to the wall) is hurt — no break.
                mobGrid.forNearby(t.x, t.y, hit.reach + 32f) { mobEntity ->
                    val mobT = with(world) { mobEntity[Transform] }
                    val mobB = with(world) { mobEntity[Body] }
                    val rx = mobT.x - t.x; val ry = mobT.y - t.y
                    val s = rx * dirX + ry * dirY
                    val mobHalf = (mobB.halfW + mobB.halfH) * 0.5f
                    if (s < -mobHalf || s > hit.reach + mobHalf) return@forNearby
                    val perp = abs(rx * dirY - ry * dirX)
                    if (perp > mobHalf + beamHalfW) return@forNearby // a fat ray clips more of the crowd
                    val mobH = with(world) { mobEntity[Health] }
                    val mobV = with(world) { mobEntity[Velocity] }
                    val mobA = with(world) { mobEntity[MobAction] }
                    val mobDodge = with(world) { mobEntity[Mob].def.dodge }
                    MobDamage.hurt(mobH, mobV, mobA, mobDodge, def.dmg * mods.gunMul * gearGun * chargeDmg, 0f, 0f, 0f, rng.nextFloat())
                }

                // Beam doesn't pierce walls: it stops at one and detonates. One-shot the hit wall and carve a
                // radius-2 blast crater at the impact point (clears walls + damages mobs), punching a hole in cover.
                if (hit.hitWall) {
                    val bx = (hit.wallTileX + 0.5f) * Tuning.TILE; val by = (hit.wallTileY + 0.5f) * Tuning.TILE
                    val r = Tuning.TILE * BEAM_BLAST_TILES * gradeBlast // v2.37: high-output beams crater wider
                    val broke = Explosion.blastWalls(map, bx, by, r)
                    mobGrid.forNearby(bx, by, r + 24f) { mobEntity ->
                        val mobT = with(world) { mobEntity[Transform] }
                        val ddx = mobT.x - bx; val ddy = mobT.y - by; val d = hypot(ddx, ddy)
                        if (d < r) {
                            val fall = 1f - d / r
                            val mobH = with(world) { mobEntity[Health] }
                            val mobV = with(world) { mobEntity[Velocity] }
                            val mobA = with(world) { mobEntity[MobAction] }
                            val mobDodge = with(world) { mobEntity[Mob].def.dodge }
                            val nx = if (d > 0f) ddx / d else dirX; val ny = if (d > 0f) ddy / d else dirY
                            MobDamage.hurt(mobH, mobV, mobA, mobDodge, BEAM_BLAST_DMG * fall, nx, ny, 240f * fall, rng.nextFloat())
                        }
                    }
                    if (broke) flow.rebuild(map, floor(t.x / Tuning.TILE).toInt(), floor(t.y / Tuning.TILE).toInt(), FlowRebuildSystem.MAX_DIST)
                    fx.addShake(0.16f, 6f); fx.spawnSparks(bx, by, 12, BEAM_SPARK)
                }
            }
            "grenade" -> {
                if (w.mag <= 0) return
                w.mag--; cd.shoot = def.fireRate * mods.fireMul * gradeFireRate
                world.entity {
                    it += Transform(x = t.x + dirX * Tuning.MUZZLE_OFFSET, y = t.y + dirY * Tuning.MUZZLE_OFFSET)
                    it += Grenade(dirX * config.player.grenadeSpeed, dirY * config.player.grenadeSpeed, config.player.grenadeFuse, blastMul = gradeBlast)
                }
            }
            else -> {
                if (w.mag <= 0) return
                w.mag--; cd.shoot = def.fireRate * mods.fireMul * gradeFireRate
                val angles = Firing.bulletAngles(aim, def.spread, def.pellets, rng)
                for (a in angles) {
                    val vx = cos(a) * config.player.bulletSpeed; val vy = sin(a) * config.player.bulletSpeed
                    world.entity {
                        it += Transform(x = t.x + cos(a) * Tuning.MUZZLE_OFFSET, y = t.y + sin(a) * Tuning.MUZZLE_OFFSET)
                        it += Bullet(vx, vy, config.player.bulletLife, def.dmg * mods.gunMul * gearGun, wallMul = gradeWall)
                    }
                }
            }
        }
    }

    companion object {
        private const val BEAM_RANGE = 1400f    // doubled (was 700) — gun range ×2
        private const val BEAM_CHARGE_TIME = 1.4f // seconds of aiming for a full charge (v2.39)
        private const val BEAM_CHARGE_DMG = 1f    // full charge = +100% damage
        private const val BEAM_W_MIN = 1.8f       // ray half-width at zero charge...
        private const val BEAM_W_MAX = 7f         // ...and fully charged (visual + pierce corridor)
        private const val BEAM_BLAST_TILES = 2f // radius-2-tile impact crater
        private const val BEAM_BLAST_DMG = 64f  // splash damage at the blast centre (0 at the rim)
        private val BEAM_SPARK = Color.valueOf("9fe8ff") // pale-cyan beam-impact sparks
    }
}
