package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.github.panda17tk.arpg.sim.Tuning

class Stamina(var value: Float = Tuning.STA_MAX, var max: Float = Tuning.STA_MAX) : Component<Stamina> {
    override fun type() = Stamina
    companion object : ComponentType<Stamina>()
}
