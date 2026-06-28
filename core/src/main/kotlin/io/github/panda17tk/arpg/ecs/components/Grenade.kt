package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** Thrown grenade: velocity + fuse (s). Explodes on solid tile or fuse end. */
class Grenade(var vx: Float, var vy: Float, var fuse: Float) : Component<Grenade> {
    override fun type() = Grenade
    companion object : ComponentType<Grenade>()
}
