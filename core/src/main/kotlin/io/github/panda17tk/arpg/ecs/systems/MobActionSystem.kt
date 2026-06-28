package io.github.panda17tk.arpg.ecs.systems

import com.badlogic.gdx.graphics.Color
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Fx
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.MobAction
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.sim.Collision
import kotlin.math.hypot
import kotlin.math.min

/** Progresses multi-frame mob actions (legacy updateMobActions): charge_melee strike, blink move, dodge timers. */
class MobActionSystem : IteratingSystem(family { all(Mob, Transform, MobAction, Body) }) {
    private val map: TileMap = world.inject()
    private val config: GameConfig = world.inject()
    private val fx: Fx = world.inject()
    private val players by lazy { world.family { all(PlayerTag, Transform, Health, Velocity) } }

    override fun onTickEntity(entity: Entity) {
        val t = entity[Transform]; val m = entity[Mob]; val a = entity[MobAction]; val b = entity[Body]
        val dt = deltaTime

        // charge_melee: wind up, then strike
        if (a.chargeT > 0f) {
            a.chargeT -= dt
            if (a.chargeT <= 0f) {
                a.chargeT = 0f
                val atk = m.def.attacks.firstOrNull { it.type == "charge_melee" }
                if (atk != null) {
                    players.forEach { e ->
                        val pt = e[Transform]; val ph = e[Health]; val pv = e[Velocity]
                        val d = hypot(pt.x - t.x, pt.y - t.y)
                        val reach = (if (atk.reach > 0f) atk.reach else atk.range) + 11f
                        if (d < reach && ph.iTime <= 0f) {
                            ph.hp -= atk.dmg
                            ph.iTime = config.ai.iFrameContact
                            val dd = d.coerceAtLeast(0.0001f)
                            pv.vx += (pt.x - t.x) / dd * atk.kb; pv.vy += (pt.y - t.y) / dd * atk.kb
                        }
                    }
                }
            }
        }

        // blink charge: brief telegraph windup, then begin the teleport
        if (a.blinkChargeT > 0f) {
            a.blinkChargeT -= dt
            if (a.blinkChargeT <= 0f) { a.blinkChargeT = 0f; a.blinkT = a.blinkTotal }
        }
        // blink: timed teleport (wall-stopped) + trailing afterimages
        if (a.blinkT > 0f) {
            fx.spawnAfterimage(t.x, t.y, m.def.w, m.def.h, Color.valueOf(m.def.color.removePrefix("#")))
            val step = min(dt, a.blinkT)
            val frac = if (a.blinkTotal > 0f) step / a.blinkTotal else 1f
            val r1 = Collision.moveAndCollide(map, t.x, t.y, b.halfW, b.halfH, a.blinkDx * frac, 0f)
            val r2 = Collision.moveAndCollide(map, r1.x, r1.y, b.halfW, b.halfH, 0f, a.blinkDy * frac)
            t.x = r2.x; t.y = r2.y
            a.blinkT -= step
            if (a.blinkT <= 0f) a.blinkT = 0f
        }

        // dodge timers
        if (a.dodgeT > 0f) a.dodgeT -= dt
        if (a.dodgeCd > 0f) a.dodgeCd -= dt
        // boss enrage / guard timers (Phase 6c)
        if (a.enrageT > 0f) a.enrageT -= dt
        if (a.guardT > 0f) a.guardT -= dt
    }
}
