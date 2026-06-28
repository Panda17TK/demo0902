package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.input.InputState

class WeaponSwitchSystem : IteratingSystem(family { all(PlayerTag, Arsenal) }) {
    private val input: InputState = world.inject()
    override fun onTickEntity(entity: Entity) {
        val slot = input.weaponSlot
        if (slot < 0) return
        val arsenal = entity[Arsenal]
        if (slot < arsenal.weapons.size) arsenal.curW = slot
    }
}
