package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** Player bullet: velocity, remaining life (s), damage. (Mob hits wire in Phase 5.) */
class Bullet(var vx: Float, var vy: Float, var life: Float, var dmg: Float) : Component<Bullet> {
    override fun type() = Bullet
    companion object : ComponentType<Bullet>()
}
