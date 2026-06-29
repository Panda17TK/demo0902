package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Buff
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mods
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.sim.Collision
import io.github.panda17tk.arpg.sim.Locomotion
import kotlin.math.hypot
import kotlin.math.pow

class MovementSystem : IteratingSystem(family { all(PlayerTag, Transform, Facing, Stamina, Body, Velocity, Mods, Health, Buff) }) {
    private val input: InputState = world.inject()
    private val map: TileMap = world.inject()
    private val config: GameConfig = world.inject()

    override fun onTickEntity(entity: Entity) {
        val t = entity[Transform]
        val f = entity[Facing]
        val s = entity[Stamina]
        val b = entity[Body]
        val v = entity[Velocity]
        val mods = entity[Mods]
        val h = entity[Health]
        val tag = entity[PlayerTag]
        val bf = entity[Buff]
        val dt = deltaTime
        if (h.iTime > 0f) h.iTime -= dt // tick down invuln (fixes stuck pink/invincible)
        if (h.hitFlash > 0f) h.hitFlash -= dt
        if (bf.staminaInfT > 0f) bf.staminaInfT -= dt
        if (bf.dashUpT > 0f) bf.dashUpT -= dt
        val staInf = bf.staminaInfT > 0f // pickup buff: dash freely without draining stamina

        val mv = Locomotion.keyboardDirection(input.left, input.right, input.up, input.down)
        val dashing = (staInf || !s.overheat) && Locomotion.isDashing(input.dash, mv.isMoving, if (staInf) 1f else s.value)
        tag.dashing = dashing
        val spd = Locomotion.speed(dashing, config.player) * mods.moveMul * (if (bf.dashUpT > 0f) DASH_UP_MUL else 1f)

        val dx = mv.dirX * spd * mv.speedScale * dt
        val dy = mv.dirY * spd * mv.speedScale * dt

        // Decay knockback velocity (very fast: gone in ~0.1s) — keeps hits punchy.
        v.vx *= 0.0001f.pow(dt)
        v.vy *= 0.0001f.pow(dt)
        // Space dash inertia: a separate drift that builds while dashing and coasts (slow decay).
        if (dashing && mv.isMoving) {
            v.driftX += mv.dirX * config.player.dashThrust * dt
            v.driftY += mv.dirY * config.player.dashThrust * dt
        }
        v.driftX *= 0.72f.pow(dt); v.driftY *= 0.72f.pow(dt) // slower decay → stronger glide (inertia)
        val dsp = hypot(v.driftX, v.driftY)
        if (dsp > 360f) { v.driftX = v.driftX / dsp * 360f; v.driftY = v.driftY / dsp * 360f }

        // Integrate input movement + knockback + dash drift through collision
        val r = Collision.moveAndCollide(map, t.x, t.y, b.halfW, b.halfH, dx + (v.vx + v.driftX) * dt, dy + (v.vy + v.driftY) * dt)
        t.x = r.x
        t.y = r.y
        if (r.hitX) { v.vx = 0f; v.driftX = 0f } // stop shoving into a wall (fixes edge stick)
        if (r.hitY) { v.vy = 0f; v.driftY = 0f }
        if (input.aiming) { f.x = input.aimX; f.y = input.aimY } // right stick aims independent of movement
        else if (mv.isMoving) { f.x = mv.dirX; f.y = mv.dirY }
        if (staInf) {
            s.value = s.max; s.overheat = false
        } else {
            s.value = Locomotion.nextStamina(s.value, dashing, dt, config.player)
            if (s.value <= 0.05f) s.overheat = true            // fully drained → overheat (no stamina actions)
            if (s.value >= s.max - 0.05f) s.overheat = false   // fully recovered → clear overheat
        }
    }

    companion object {
        private const val DASH_UP_MUL = 1.5f // dash-speed pickup buff
    }
}
