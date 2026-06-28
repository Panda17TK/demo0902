package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** Multi-frame action + dodge state for a mob (legacy m._charge / m._blink / dodge timers). */
class MobAction(
    var chargeT: Float = 0f,
    var chargeDx: Float = 0f,
    var chargeDy: Float = 0f,
    var blinkT: Float = 0f,
    var blinkTotal: Float = 0f,
    var blinkDx: Float = 0f,
    var blinkDy: Float = 0f,
    var dodgeT: Float = 0f,
    var dodgeCd: Float = 0f,
) : Component<MobAction> {
    val charging: Boolean get() = chargeT > 0f
    val blinking: Boolean get() = blinkT > 0f
    override fun type() = MobAction
    companion object : ComponentType<MobAction>()
}
