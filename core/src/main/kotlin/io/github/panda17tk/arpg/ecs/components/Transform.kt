package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** Position plus previous-step position for render interpolation. */
class Transform(
    var x: Float = 0f,
    var y: Float = 0f,
    var prevX: Float = 0f,
    var prevY: Float = 0f,
) : Component<Transform> {
    override fun type() = Transform
    companion object : ComponentType<Transform>()
}
