package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** Timed player buffs from pickups/consumables (seconds remaining; 0 = inactive). Applied in
 *  MovementSystem (+ PickupSystem for magnetT). v2.35 adds the timed resistances: heat/cold-proof
 *  coatings, the field magnet, and the regen patch. */
class Buff(
    var staminaInfT: Float = 0f,
    var dashUpT: Float = 0f,
    var heatProofT: Float = 0f,
    var coldProofT: Float = 0f,
    var magnetT: Float = 0f,
    var regenT: Float = 0f,
) : Component<Buff> {
    override fun type() = Buff
    companion object : ComponentType<Buff>()
}
