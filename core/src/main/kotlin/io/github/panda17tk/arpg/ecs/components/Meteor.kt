package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** v2.87 流星群: one falling rock — [fall] seconds of telegraphed shadow before the impact. */
class Meteor(var fall: Float) : Component<Meteor> {
    override fun type() = Meteor
    companion object : ComponentType<Meteor>()
}
