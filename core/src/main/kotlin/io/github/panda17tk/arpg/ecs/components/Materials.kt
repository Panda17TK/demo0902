package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.github.panda17tk.arpg.sim.Tuning

/** Build materials used to place walls (legacy player.inv.blocks) + 星屑 (v2.43: the trade currency). */
class Materials(var blocks: Int = Tuning.START_MATERIALS, var dust: Int = 0) : Component<Materials> {
    override fun type() = Materials
    companion object : ComponentType<Materials>()
}
