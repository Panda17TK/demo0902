package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** Timed player buffs from pickups (seconds remaining; 0 = inactive). Applied in MovementSystem. */
class Buff(var staminaInfT: Float = 0f, var dashUpT: Float = 0f) : Component<Buff> {
    override fun type() = Buff
    companion object : ComponentType<Buff>()
}
