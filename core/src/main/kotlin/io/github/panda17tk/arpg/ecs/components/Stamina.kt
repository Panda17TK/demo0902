package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.github.panda17tk.arpg.sim.Tuning

/** Player stamina: dashing drains it, not-dashing regenerates it (see [io.github.panda17tk.arpg.sim.Locomotion]). */
class Stamina(var value: Float = Tuning.STA_MAX, var max: Float = Tuning.STA_MAX) : Component<Stamina> {
    override fun type() = Stamina
    companion object : ComponentType<Stamina>()
}
