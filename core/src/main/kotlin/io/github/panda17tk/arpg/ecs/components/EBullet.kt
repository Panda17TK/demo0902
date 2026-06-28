package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** Enemy bullet (legacy state.ebullets). homing>0 turns toward the player; mine stays put. */
class EBullet(
    var vx: Float,
    var vy: Float,
    var life: Float,
    var dmg: Float,
    var homing: Float = 0f,
    var mine: Boolean = false,
) : Component<EBullet> {
    override fun type() = EBullet
    companion object : ComponentType<EBullet>()
}
