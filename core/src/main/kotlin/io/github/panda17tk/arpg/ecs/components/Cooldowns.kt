package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** Per-player action cooldowns (seconds remaining). beamCharge (v2.39): 0..1, builds while aiming a beam. */
class Cooldowns(
    var shoot: Float = 0f, var melee: Float = 0f, var beamCharge: Float = 0f, var blink: Float = 0f,
    var chargeId: String = "", // v2.106: which weapon owns beamCharge — switching drops the carry
) : Component<Cooldowns> {
    override fun type() = Cooldowns
    companion object : ComponentType<Cooldowns>()
}
