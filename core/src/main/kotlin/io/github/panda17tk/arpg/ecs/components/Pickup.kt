package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** A floor drop the player auto-collects by walking near it. kind: ammo9/ammo12/ammoBeam/ammoNade/blocks/med. */
class Pickup(val kind: String, val amount: Int, var t: Float = 0f) : Component<Pickup> {
    override fun type() = Pickup
    companion object : ComponentType<Pickup>()
}
