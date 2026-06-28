package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

class Health(var hp: Float, var hpMax: Float, var iTime: Float = 0f, var hitFlash: Float = 0f) : Component<Health> {
    override fun type() = Health
    companion object : ComponentType<Health>()
}
