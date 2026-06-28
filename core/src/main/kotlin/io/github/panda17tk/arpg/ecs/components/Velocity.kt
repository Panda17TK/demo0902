package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class Velocity(var vx: Float = 0f, var vy: Float = 0f, var driftX: Float = 0f, var driftY: Float = 0f) : Component<Velocity> {
    override fun type() = Velocity
    companion object : ComponentType<Velocity>()
}
