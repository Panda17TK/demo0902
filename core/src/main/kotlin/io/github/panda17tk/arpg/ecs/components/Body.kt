package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** AABB half-extents for tile collision. */
class Body(var halfW: Float, var halfH: Float) : Component<Body> {
    override fun type() = Body
    companion object : ComponentType<Body>()
}
