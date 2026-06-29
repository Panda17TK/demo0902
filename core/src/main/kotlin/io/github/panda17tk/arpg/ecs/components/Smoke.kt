package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** A smoke cloud (from a smoke-bomb pickup): lives [life]s and erases enemy bullets within [radius]. */
class Smoke(val radius: Float, val life: Float, var t: Float = 0f) : Component<Smoke> {
    override fun type() = Smoke
    companion object : ComponentType<Smoke>()
}
