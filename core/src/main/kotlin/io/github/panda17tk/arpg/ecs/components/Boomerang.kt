package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity

/**
 * v2.101 帰還刃: the thrown blade. Outbound it rides [vx]/[vy] and slows; when [outT] runs dry
 * (or it kisses a wall) it turns [returning] and homes on the keeper, cutting again on the way
 * back. Each leg damages a mob at most once ([hit] clears at the turn). [life] is the hard cap
 * so a blade can never orbit forever; the catch is handled by BoomerangSystem.
 */
class Boomerang(
    var vx: Float,
    var vy: Float,
    var dmg: Float,
    var outT: Float,
    var returning: Boolean = false,
    val hit: MutableSet<Entity> = mutableSetOf(),
    var spin: Float = 0f, // cosmetic — the renderer's blade angle
    var life: Float = 8f,
) : Component<Boomerang> {
    override fun type() = Boomerang
    companion object : ComponentType<Boomerang>()
}
