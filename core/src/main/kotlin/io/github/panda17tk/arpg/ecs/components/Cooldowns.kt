package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** Per-player action cooldowns (seconds remaining). */
class Cooldowns(var shoot: Float = 0f, var melee: Float = 0f) : Component<Cooldowns> {
    override fun type() = Cooldowns
    companion object : ComponentType<Cooldowns>()
}
