package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** Marker for the player-controlled entity. */
class PlayerTag : Component<PlayerTag> {
    override fun type() = PlayerTag
    companion object : ComponentType<PlayerTag>()
}
