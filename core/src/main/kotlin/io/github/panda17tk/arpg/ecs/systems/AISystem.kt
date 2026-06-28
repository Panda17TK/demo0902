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
import io.github.panda17tk.arpg.sim.Collision
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt

/** Enemy AI: flow-field chase + separation + contact + data-driven attack dispatch (legacy ai.js + runAttacks). */
class AISystem(private val mobGrid: SpatialGrid<Entity>) :
    IteratingSystem(family { all(Mob, Transform, Velocity, Body, Health, Facing, MobAction) }) {

    private val map: TileMap = world.inject()
    private val flow: FlowField = world.inject()
    private val config: GameConfig = world.inject()
    private val rng: Rng = world.inject()

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

        // Separation
        var sepX = 0f; var sepY = 0f
        mobGrid.forNearby(t.x, t.y, ai.sepRadius) { other ->
            if (other == entity) return@forNearby
            val ot = with(world) { other[Transform] }
            val ddx = t.x - ot.x; val ddy = t.y - ot.y
            val d = hypot(ddx, ddy)
            if (d in 0.0001f..ai.sepRadius) {
                val w = (ai.sepRadius - d) / ai.sepRadius
                sepX += ddx / d * w; sepY += ddy / d * w
            }
        }
        if (sepX != 0f || sepY != 0f) {
            val l = sqrt(sepX * sepX + sepY * sepY)
            v.vx += sepX / l * eff * 0.5f; v.vy += sepY / l * eff * 0.5f
        }

        // Movement: flow-field follow, else LOS-direct
        var mvx = 0f; var mvy = 0f
        val (fx, fy) = AiMove.followDir(map, flow, t.x, t.y)
        if (fx != 0f || fy != 0f) {
            mvx = fx * eff * dt; mvy = fy * eff * dt; f.x = fx; f.y = fy
        } else if (see && dist > 0f) {
            mvx = dx / dist * eff * dt; mvy = dy / dist * eff * dt
            f.x = dx / dist; f.y = dy / dist
        }
        val r1 = Collision.moveAndCollide(map, t.x, t.y, b.halfW, b.halfH, mvx + v.vx * dt, 0f)
        val r2 = Collision.moveAndCollide(map, r1.x, r1.y, b.halfW, b.halfH, 0f, mvy + v.vy * dt)
        t.x = r2.x; t.y = r2.y

        val pt = playerT; val ph = playerH; val pv = playerV

        // Contact damage + knockback
        if (pt != null && ph != null && pv != null && m.bumpCd <= 0f) {
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
            m.def.attacks.forEachIndexed { i, atk ->
                if (m.attackCd[i] <= 0f) {
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
}
