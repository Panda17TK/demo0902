package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** A creature's speech bubble (Living Planets). Only canSpeak creatures emit lines; [remaining] times the bubble. */
class Speech(
    val canSpeak: Boolean = false,
    var text: String = "",
    var remaining: Float = 0f,
    var cooldown: Float = 0f,
) : Component<Speech> {
    override fun type() = Speech
    companion object : ComponentType<Speech>()
}
