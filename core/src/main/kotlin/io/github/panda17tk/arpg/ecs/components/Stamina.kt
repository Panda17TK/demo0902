package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** Player stamina: dashing drains it, not-dashing regenerates it (see [io.github.panda17tk.arpg.sim.Locomotion]). */
class Stamina(var value: Float = 100f, var max: Float = 100f) : Component<Stamina> {
    override fun type() = Stamina
    companion object : ComponentType<Stamina>()
}
