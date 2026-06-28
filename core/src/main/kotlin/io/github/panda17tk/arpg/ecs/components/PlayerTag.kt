package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** Marker for the player-controlled entity. [dashing] is published by MovementSystem each tick. */
class PlayerTag(var dashing: Boolean = false) : Component<PlayerTag> {
    override fun type() = PlayerTag
    companion object : ComponentType<PlayerTag>()
}
