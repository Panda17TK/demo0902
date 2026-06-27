package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.sim.Locomotion

class MovementSystem : IteratingSystem(family { all(PlayerTag, Transform, Facing, Stamina) }) {
    private val input: InputState = world.inject()

    override fun onTickEntity(entity: Entity) {
        val t = entity[Transform]
        val f = entity[Facing]
        val s = entity[Stamina]
        val dt = deltaTime

        val mv = Locomotion.keyboardDirection(input.left, input.right, input.up, input.down)
        val dashing = Locomotion.isDashing(input.dash, mv.isMoving, s.value)
        val spd = Locomotion.speed(dashing)

        t.x += mv.dirX * spd * mv.speedScale * dt
        t.y += mv.dirY * spd * mv.speedScale * dt
        if (mv.isMoving) { f.x = mv.dirX; f.y = mv.dirY }
        s.value = Locomotion.nextStamina(s.value, dashing, dt)
    }
}
