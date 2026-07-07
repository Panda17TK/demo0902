package io.github.panda17tk.arpg.ecs.systems

import com.badlogic.gdx.graphics.Color
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.EBullet
import io.github.panda17tk.arpg.ecs.components.Fx
import io.github.panda17tk.arpg.ecs.components.Gear
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin

/** Enemy bullet update (legacy updateEnemyBullets): homing/move, despawn on wall/expiry, damage the player. */
class EBulletSystem : IteratingSystem(family { all(EBullet, Transform) }) {
    private val map: TileMap = world.inject()
    private val difficulty: io.github.panda17tk.arpg.sim.Difficulty = world.inject() // v2.97
    private val rng: Rng = world.inject()
    private val fx: Fx = world.inject()
    private val deflectColor = Color.valueOf("9ec5ff")
    private val players by lazy { world.family { all(PlayerTag, Transform, Health, Velocity, Body) } }

    override fun onTickEntity(entity: Entity) {
        val t = entity[Transform]
        val b = entity[EBullet]
        val dt = deltaTime

        var pt: Transform? = null; var ph: Health? = null; var pv: Velocity? = null; var pb: Body? = null; var pdash = false
        var pDmgMul = 1f // v2.33: armor — scales enemy-bullet damage to the player
        players.forEach { e ->
            pt = e[Transform]; ph = e[Health]; pv = e[Velocity]; pb = e[Body]; pdash = e[PlayerTag].dashing
            pDmgMul = (e.getOrNull(Gear)?.loadout?.damageTakenMul ?: 1f) * difficulty.dmgTakenMul * (e.getOrNull(io.github.panda17tk.arpg.ecs.components.Mods)?.armorMul ?: 1f) // v2.97; v2.107
        }

        if (b.homing > 0f && pt != null) {
            val twoPi = (Math.PI * 2.0).toFloat()
            val pi = Math.PI.toFloat()
            val want = atan2(pt!!.y - t.y, pt!!.x - t.x)
            val cur = atan2(b.vy, b.vx)
            val diff = ((want - cur + pi * 3f) % twoPi) - pi
            val turn = diff.coerceIn(-b.homing * dt, b.homing * dt)
            val sp = hypot(b.vx, b.vy)
            val na = cur + turn
            b.vx = cos(na) * sp; b.vy = sin(na) * sp
        }
        if (!b.mine) { t.x += b.vx * dt; t.y += b.vy * dt }
        b.life -= dt

        val tx = floor(t.x / Tuning.TILE).toInt(); val ty = floor(t.y / Tuning.TILE).toInt()
        if ((!b.mine && map.solidAt(tx, ty)) || b.life <= 0f) { world -= entity; return }

        val p = pt; val h = ph; val vel = pv; val body = pb
        if (p != null && h != null && vel != null && body != null) {
            val hitR = if (b.mine) 14f else 3f
            if (abs(p.x - t.x) < body.halfW + hitR && abs(p.y - t.y) < body.halfH + hitR) {
                if (pdash && rng.nextFloat() < 0.30f) { // dashing → 30% chance to deflect (no damage)
                    val d = hypot(t.x - p.x, t.y - p.y).coerceAtLeast(0.0001f)
                    val nx = (t.x - p.x) / d; val ny = (t.y - p.y) / d
                    val sp = hypot(b.vx, b.vy).coerceAtLeast(220f) * 1.3f
                    b.vx = nx * sp; b.vy = ny * sp; b.homing = 0f
                    t.x += nx * 10f; t.y += ny * 10f
                    fx.spawnSparks(t.x, t.y, 6, deflectColor)
                    return
                }
                if (h.iTime <= 0f) {
                    h.hp -= b.dmg * pDmgMul; h.iTime = 0.8f
                    val d = hypot(p.x - t.x, p.y - t.y).coerceAtLeast(0.0001f)
                    vel.vx += (p.x - t.x) / d * 180f; vel.vy += (p.y - t.y) / d * 180f
                }
                world -= entity
            }
        }
    }
}
