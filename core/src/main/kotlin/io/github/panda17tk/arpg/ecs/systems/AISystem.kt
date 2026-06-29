package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ai.AiMove
import io.github.panda17tk.arpg.combat.MobAttacks
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.MobAction
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.pathfinding.FlowField
import io.github.panda17tk.arpg.pathfinding.Los
import io.github.panda17tk.arpg.pathfinding.SpatialGrid
import io.github.panda17tk.arpg.sim.CircleCollision
import io.github.panda17tk.arpg.sim.Collision
import io.github.panda17tk.arpg.sim.Leveling
import io.github.panda17tk.arpg.sim.PlanetField
import io.github.panda17tk.arpg.sim.Tribes
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Enemy AI: flow-field chase + separation + contact + data-driven attack dispatch (legacy ai.js + runAttacks). */
class AISystem(private val mobGrid: SpatialGrid<Entity>) :
    IteratingSystem(family { all(Mob, Transform, Velocity, Body, Health, Facing, MobAction) }) {

    private val map: TileMap = world.inject()
    private val flow: FlowField = world.inject()
    private val config: GameConfig = world.inject()
    private val rng: Rng = world.inject()
    private val tribes: Tribes = world.inject()
    private val planetField: PlanetField = world.inject()

    private val players by lazy { world.family { all(PlayerTag, Transform, Health, Velocity) } }

    override fun onTickEntity(entity: Entity) {
        val t = entity[Transform]
        val v = entity[Velocity]
        val b = entity[Body]
        val m = entity[Mob]
        val f = entity[Facing]
        val h = entity[Health]
        val action = entity[MobAction]
        val dt = deltaTime
        val ai = config.ai

        // Decay knockback velocity + timers (always)
        val decay = 0.02f.pow(dt)
        v.vx *= decay; v.vy *= decay
        // Decay gravity/crash momentum (drift) lightly so flung mobs coast, then AI reasserts control.
        val driftDecay = MOB_DRIFT_DECAY.pow(dt)
        v.driftX *= driftDecay; v.driftY *= driftDecay
        if (m.bumpCd > 0f) m.bumpCd -= dt
        if (h.hitFlash > 0f) h.hitFlash -= dt
        for (i in m.attackCd.indices) if (m.attackCd[i] > 0f) m.attackCd[i] -= dt

        // While charging/blinking, MobActionSystem drives the mob; skip normal AI.
        if (action.charging || action.blinking) return

        // Player data
        var px = t.x; var py = t.y
        var playerT: Transform? = null; var playerH: Health? = null; var playerV: Velocity? = null
        players.forEach { e ->
            playerT = e[Transform]; playerH = e[Health]; playerV = e[Velocity]
            px = e[Transform].x; py = e[Transform].y
        }

        val slow = if (m.tier == "normal" && h.hp <= h.hpMax * 0.5f) ai.hpSlowMul else 1f
        val enrage = if (action.enrageT > 0f) action.enrageMul else 1f
        val eff = m.speed * slow * enrage

        val dx = px - t.x; val dy = py - t.y
        val dist = hypot(dx, dy)
        val see = dist < m.def.seeRange && Los.hasLineOfSight(map, t.x, t.y, px, py)
        val toPx = if (dist > 0f) dx / dist else 1f
        val toPy = if (dist > 0f) dy / dist else 0f

        // Neighbour scan (one pass): separation (any mob) + same-tribe cohesion + nearest hostile-tribe mob.
        val myTribe = m.tribe
        var sepX = 0f; var sepY = 0f
        var cohX = 0f; var cohY = 0f; var cohN = 0
        var hostX = 0f; var hostY = 0f; var hostD = Float.MAX_VALUE; var hostFound = false
        var hostHp: Health? = null; var hostV: Velocity? = null
        mobGrid.forNearby(t.x, t.y, COH_RADIUS) { other ->
            if (other == entity) return@forNearby
            with(world) {
                val ot = other[Transform]; val om = other[Mob]
                val ddx = t.x - ot.x; val ddy = t.y - ot.y
                val d = hypot(ddx, ddy)
                if (d < 0.0001f) return@forNearby
                if (d <= ai.sepRadius) { val w = (ai.sepRadius - d) / ai.sepRadius; sepX += ddx / d * w; sepY += ddy / d * w }
                if (tribes.areHostile(myTribe, om.tribe)) {
                    if (d < hostD) { hostD = d; hostX = ot.x; hostY = ot.y; hostFound = true; hostHp = other[Health]; hostV = other[Velocity] }
                } else if (om.tribe == myTribe) {
                    cohX += ot.x; cohY += ot.y; cohN++
                }
            }
        }
        if (sepX != 0f || sepY != 0f) {
            val l = sqrt(sepX * sepX + sepY * sepY)
            v.vx += sepX / l * eff * 0.5f; v.vy += sepY / l * eff * 0.5f
        }
        // Cohesion: drift toward the same-tribe centroid when it isn't already crowding us (herding).
        if (cohN > 0) {
            val ccx = cohX / cohN - t.x; val ccy = cohY / cohN - t.y
            val cl = hypot(ccx, ccy)
            if (cl > ai.sepRadius) { v.vx += ccx / cl * eff * COH_W; v.vy += ccy / cl * eff * COH_W }
        }

        // Tactics scale with smarts (tribe intelligence + level): smart tribes take cover + kite.
        val smarts = Leveling.smarts(tribes.intelligenceOf(myTribe), m.level)
        val hasRanged = m.def.attacks.any { it.type == "shot" }
        val brawl = hostFound && hostD <= HOSTILE_RANGE
        val cover = if (!brawl && smarts > COVER_SMARTS && see) coverDir(t.x, t.y, px, py) else null
        var mvx = 0f; var mvy = 0f
        if (brawl) {
            // Frontline: charge the rival-tribe mob.
            val bdx = hostX - t.x; val bdy = hostY - t.y; val bd = hypot(bdx, bdy)
            if (bd > 0f) { mvx = bdx / bd * eff * dt; mvy = bdy / bd * eff * dt; f.x = bdx / bd; f.y = bdy / bd }
        } else if (cover != null) {
            // Smart: slip toward a wall to shield from the player, still facing them to fire.
            mvx = cover[0] * eff * dt; mvy = cover[1] * eff * dt; f.x = toPx; f.y = toPy
        } else if (smarts > KITE_SMARTS && hasRanged && see && dist in 1f..KITE_DIST) {
            // Ranged support kites: back off and support from behind the frontline.
            mvx = -toPx * eff * dt; mvy = -toPy * eff * dt; f.x = toPx; f.y = toPy
        } else {
            val (fx, fy) = AiMove.followDir(map, flow, t.x, t.y)
            if (fx != 0f || fy != 0f) {
                mvx = fx * eff * dt; mvy = fy * eff * dt; f.x = fx; f.y = fy
            } else if (see && dist > 0f) {
                mvx = dx / dist * eff * dt; mvy = dy / dist * eff * dt; f.x = dx / dist; f.y = dy / dist
            }
        }
        // Drift (gravity/crash momentum) rides on top of AI movement + knockback; zero it on wall hits.
        val r1 = Collision.moveAndCollide(map, t.x, t.y, b.halfW, b.halfH, mvx + (v.vx + v.driftX) * dt, 0f)
        if (r1.hitX) v.driftX = 0f
        val r2 = Collision.moveAndCollide(map, r1.x, r1.y, b.halfW, b.halfH, 0f, mvy + (v.vy + v.driftY) * dt)
        if (r2.hitY) v.driftY = 0f
        t.x = r2.x; t.y = r2.y
        // Solid planets: push the mob out of any planet circle (crash damage arrives in SP-4).
        val pc = CircleCollision.resolve(t.x, t.y, b.halfW, v.driftX, v.driftY, planetField.planets)
        if (pc.hit) { t.x = pc.x; t.y = pc.y; v.driftX = 0f; v.driftY = 0f }

        val pt = playerT; val ph = playerH; val pv = playerV

        // Contact damage: brawl a hostile-tribe mob if we're locked onto one, else bump the player.
        if (brawl && m.bumpCd <= 0f) {
            val hh = hostHp; val hv = hostV
            if (hh != null && hv != null && hostD < (b.halfW + 14f)) {
                val nl = hostD.coerceAtLeast(0.0001f)
                hh.hp -= MOB_VS_MOB_DMG
                hv.vx += (hostX - t.x) / nl * m.def.contactKB; hv.vy += (hostY - t.y) / nl * m.def.contactKB
                v.vx -= (hostX - t.x) / nl * m.def.contactKB * 0.5f; v.vy -= (hostY - t.y) / nl * m.def.contactKB * 0.5f
                m.bumpCd = 0.4f
                if (hh.hp <= 0f) { // killing blow on a rival tribe → gain XP, level up past the threshold
                    m.xp += Leveling.xpForKill(m.level)
                    while (m.xp >= Leveling.threshold(m.level)) { m.xp -= Leveling.threshold(m.level); m.level++ }
                }
            }
        } else if (pt != null && ph != null && pv != null && m.bumpCd <= 0f) {
            if (abs(t.x - pt.x) < (b.halfW + 11f) && abs(t.y - pt.y) < (b.halfH + 11f)) {
                val nx = if (dist > 0f) (pt.x - t.x) / dist else 1f
                val ny = if (dist > 0f) (pt.y - t.y) / dist else 0f
                pv.vx += nx * ai.playerKnockback; pv.vy += ny * ai.playerKnockback
                v.vx -= nx * m.def.contactKB; v.vy -= ny * m.def.contactKB
                if (ph.iTime <= 0f) { ph.hp -= ai.contactDmg; ph.iTime = ai.iFrameContact }
                m.bumpCd = 0.28f
            }
        }

        // Data-driven attack dispatch (legacy runAttacks). melee/shot/lunge/charge_melee/blink in 5b.
        if (pt != null && ph != null && pv != null) {
            val usable = Leveling.attacksForLevel(m.level, m.def.attacks.size) // level unlocks more skills
            m.def.attacks.forEachIndexed { i, atk ->
                if (i < usable && m.attackCd[i] <= 0f) {
                    val executed = MobAttacks.tryAttack(
                        world, atk, m.def, t, f, v, action, h, ph, pv,
                        dist, toPx, toPy, see, ai.iFrameContact,
                        config, m.waveNum, rng,
                    )
                    // Enraged bosses attack faster too (legacy cdScale = 1/enrageMul).
                    if (executed) m.attackCd[i] = atk.cd * if (action.enrageT > 0f) 1f / action.enrageMul else 1f
                }
            }
        }
    }

    /** A step direction (away-ish from the player) that breaks the player's line of sight → wall cover. */
    private fun coverDir(x: Float, y: Float, px: Float, py: Float): FloatArray? {
        val away = atan2(y - py, x - px)
        val step = Tuning.TILE * 1.5f
        for (off in COVER_OFFSETS) {
            val a = away + off
            val nx = x + cos(a) * step; val ny = y + sin(a) * step
            if (!Los.hasLineOfSight(map, nx, ny, px, py)) return floatArrayOf(cos(a), sin(a))
        }
        return null
    }

    companion object {
        private val COH_RADIUS = Tuning.TILE * 5f // same-tribe cohesion + hostile-scan radius
        private val HOSTILE_RANGE = Tuning.TILE * 7f // lock onto a hostile-tribe mob within this range
        private const val COH_W = 0.22f // herd cohesion weight (< separation's 0.5 so herds don't collapse)
        private const val MOB_VS_MOB_DMG = 14f // contact damage between hostile tribes
        private const val COVER_SMARTS = 0.55f // smarts above this → seek wall cover
        private const val KITE_SMARTS = 0.40f // smarts above this → ranged mobs kite
        private val KITE_DIST = Tuning.TILE * 5f // back off when the player is closer than this
        private const val MOB_DRIFT_DECAY = 0.8f // gravity/crash momentum bleeds ~20%/s so AI movement dominates
        private val COVER_OFFSETS = floatArrayOf(0f, 0.6f, -0.6f, 1.2f, -1.2f) // away ± up to ~70°
    }
}
