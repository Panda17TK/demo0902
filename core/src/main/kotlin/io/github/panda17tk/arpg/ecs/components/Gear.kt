package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.github.panda17tk.arpg.item.ItemDef
import io.github.panda17tk.arpg.item.Loadout

/**
 * The player's equipment + item backpack (v2.33). Systems read the loadout's aggregate
 * multipliers each tick; the inventory screen swaps items between the two.
 */
class Gear(
    val loadout: Loadout = Loadout(),
    val backpack: MutableList<ItemDef> = mutableListOf(),
) : Component<Gear> {
    override fun type() = Gear
    companion object : ComponentType<Gear>()
}
