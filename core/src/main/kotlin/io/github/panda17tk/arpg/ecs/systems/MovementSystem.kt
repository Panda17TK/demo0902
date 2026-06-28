package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Facing
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

class MovementSystem : IteratingSystem(family { all(PlayerTag, Transform, Facing, Stamina, Body, Velocity, Mods) }) {
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
        val dt = deltaTime

        val mv = Locomotion.keyboardDirection(input.left, input.right, input.up, input.down)
        val dashing = Locomotion.isDashing(input.dash, mv.isMoving, s.value)
        val spd = Locomotion.speed(dashing, config.player) * mods.moveMul

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
        v.driftX *= 0.5f.pow(dt); v.driftY *= 0.5f.pow(dt)
        val dsp = hypot(v.driftX, v.driftY)
        if (dsp > 220f) { v.driftX = v.driftX / dsp * 220f; v.driftY = v.driftY / dsp * 220f }

        // Integrate input movement + knockback + dash drift through collision
        val r = Collision.moveAndCollide(map, t.x, t.y, b.halfW, b.halfH, dx + (v.vx + v.driftX) * dt, dy + (v.vy + v.driftY) * dt)
        t.x = r.x
        t.y = r.y
        if (input.aiming) { f.x = input.aimX; f.y = input.aimY } // right stick aims independent of movement
        else if (mv.isMoving) { f.x = mv.dirX; f.y = mv.dirY }
        s.value = Locomotion.nextStamina(s.value, dashing, dt, config.player)
    }
}
