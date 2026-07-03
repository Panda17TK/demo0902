package io.github.panda17tk.arpg.ecs.world

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.Gear
import io.github.panda17tk.arpg.item.EquipSlot
import io.github.panda17tk.arpg.item.Loadout

/**
 * Equipment mutations driven from the inventory overlay (v2.33). Tapping a slot cycles it to the
 * next compatible backpack item (the previous piece goes back into the backpack); the RANGED slot
 * is kept in step with [Arsenal] so equipping a gun IS switching weapons.
 */
object GearOps {
    /**
     * Cycle [slot] to the first compatible item in the backpack. Returns the newly-equipped item's
     * name, or null when the backpack holds nothing that fits (the slot is left untouched — the
     * four core slots never go empty).
     */
    fun cycleSlot(world: World, player: Entity, slot: EquipSlot): String? = with(world) {
        val gear = player.getOrNull(Gear) ?: return null
        val idx = gear.backpack.indexOfFirst { Loadout.compatible(slot, it.kind) }
        if (idx < 0) return null
        val item = gear.backpack.removeAt(idx)
        val prev = gear.loadout.set(slot, item)
        if (prev != null) gear.backpack.add(prev)
        if (slot == EquipSlot.RANGED) syncArsenal(world, player, item.weaponType)
        item.name
    }

    /** Point [Arsenal] at the WeaponDef the newly-equipped gun behaves as. */
    private fun syncArsenal(world: World, player: Entity, weaponType: String?) = with(world) {
        if (weaponType == null) return@with
        val arsenal = player.getOrNull(Arsenal) ?: return@with
        val w = arsenal.weapons.indexOfFirst { it.def.id == weaponType }
        if (w >= 0) arsenal.curW = w
    }
}
