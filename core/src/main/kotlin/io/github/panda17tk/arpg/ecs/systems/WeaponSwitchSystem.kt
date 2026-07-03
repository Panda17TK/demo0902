package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.Gear
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.item.EquipSlot
import io.github.panda17tk.arpg.item.ItemKind

class WeaponSwitchSystem : IteratingSystem(family { all(PlayerTag, Arsenal) }) {
    private val input: InputState = world.inject()
    override fun onTickEntity(entity: Entity) {
        val slot = input.weaponSlot
        if (slot < 0) return
        val arsenal = entity[Arsenal]
        if (slot >= arsenal.weapons.size) return
        arsenal.curW = slot
        // v2.33: the number keys and the equipment RANGED slot are two views of one choice — keep
        // the loadout in step. v2.37 (grades): an equipped gun of the selected TYPE stays put (a
        // graded mg is still an mg), and we only ever equip guns the backpack actually holds —
        // no conjuring catalog items the player doesn't own.
        val gear = entity.getOrNull(Gear) ?: return
        val defId = arsenal.weapons[slot].def.id
        if (gear.loadout.ranged?.weaponType == defId) return
        val idx = gear.backpack.indexOfFirst { it.kind == ItemKind.RANGED_WEAPON && it.weaponType == defId }
        if (idx < 0) return
        val item = gear.backpack.removeAt(idx)
        val prev = gear.loadout.set(EquipSlot.RANGED, item)
        if (prev != null) gear.backpack.add(prev)
    }
}
