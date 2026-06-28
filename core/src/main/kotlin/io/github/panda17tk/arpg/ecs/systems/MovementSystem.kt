package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.sim.Collision
import io.github.panda17tk.arpg.sim.Locomotion
import kotlin.math.pow

class MovementSystem : IteratingSystem(family { all(PlayerTag, Transform, Facing, Stamina, Body, Velocity) }) {
    private val input: InputState = world.inject()
    private val map: TileMap = world.inject()
    private val config: GameConfig = world.inject()

    override fun onTickEntity(entity: Entity) {
        val t = entity[Transform]
        val f = entity[Facing]
        val s = entity[Stamina]
        val b = entity[Body]
        val v = entity[Velocity]
        val dt = deltaTime

        val mv = Locomotion.keyboardDirection(input.left, input.right, input.up, input.down)
        val dashing = Locomotion.isDashing(input.dash, mv.isMoving, s.value)
        val spd = Locomotion.speed(dashing, config.player)

        val dx = mv.dirX * spd * mv.speedScale * dt
        val dy = mv.dirY * spd * mv.speedScale * dt

        // Decay knockback velocity (very fast decay: 0.0001^dt ≈ gone in ~0.1s)
        v.vx *= 0.0001f.pow(dt)
        v.vy *= 0.0001f.pow(dt)

        // Integrate input movement + knockback together through collision
        val r = Collision.moveAndCollide(map, t.x, t.y, b.halfW, b.halfH, dx + v.vx * dt, dy + v.vy * dt)
        t.x = r.x
        t.y = r.y
        if (mv.isMoving) { f.x = mv.dirX; f.y = mv.dirY }
        s.value = Locomotion.nextStamina(s.value, dashing, dt, config.player)
    }
}
