package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.github.panda17tk.arpg.combat.WeaponDef

/** Runtime weapon: its definition, current magazine, and time since last shot (auto-reload). */
class WeaponRuntime(val def: WeaponDef, var mag: Int, var autoReloadTimer: Float = 0f, var reloadT: Float = 0f)

class Arsenal(val weapons: List<WeaponRuntime>, var curW: Int = 0) : Component<Arsenal> {
    val current: WeaponRuntime get() = weapons[curW]
    override fun type() = Arsenal
    companion object : ComponentType<Arsenal>()
}
