package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.Gear
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.item.EquipSlot
import io.github.panda17tk.arpg.item.ItemCatalog

class WeaponSwitchSystem : IteratingSystem(family { all(PlayerTag, Arsenal) }) {
    private val input: InputState = world.inject()
    override fun onTickEntity(entity: Entity) {
        val slot = input.weaponSlot
        if (slot < 0) return
        val arsenal = entity[Arsenal]
        if (slot >= arsenal.weapons.size) return
        arsenal.curW = slot
        // v2.33: the number keys and the equipment RANGED slot are two views of one choice — keep
        // the loadout in step (the previously-equipped gun goes back into the backpack).
        val gear = entity.getOrNull(Gear) ?: return
        val item = ItemCatalog.byWeaponType(arsenal.weapons[slot].def.id) ?: return
        if (gear.loadout.ranged?.id == item.id) return
        gear.backpack.removeAll { it.id == item.id }
        val prev = gear.loadout.set(EquipSlot.RANGED, item)
        if (prev != null && prev.id != item.id) gear.backpack.add(prev)
    }
}
