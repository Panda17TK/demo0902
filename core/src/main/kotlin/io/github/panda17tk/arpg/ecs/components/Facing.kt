package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** Unit facing direction (defaults to +x). */
class Facing(var x: Float = 1f, var y: Float = 0f) : Component<Facing> {
    override fun type() = Facing
    companion object : ComponentType<Facing>()
}
