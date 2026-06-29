package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.github.panda17tk.arpg.config.FamilyRole
import io.github.panda17tk.arpg.sim.CreatureState

/** A mob's living temperament + current behavioural state (Living Planets). Legacy defaults = always hostile. */
class CreatureMind(
    val intelligence: Float = 0f,
    val bravery: Float = 1f,
    val protectiveness: Float = 0f,
    val mercyThreshold: Float = 0f,
    val canBeg: Boolean = false,
    val canHideAndRest: Boolean = false,
    val familyRole: FamilyRole = FamilyRole.NONE,
    var state: CreatureState = CreatureState.Hostile,
    var stateTimer: Float = 0f,
) : Component<CreatureMind> {
    override fun type() = CreatureMind
    companion object : ComponentType<CreatureMind>()
}
