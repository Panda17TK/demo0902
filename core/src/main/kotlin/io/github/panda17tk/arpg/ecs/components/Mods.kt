package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/**
 * Permanent per-run player upgrades (legacy player.mods). All multipliers default to the
 * identity (1) so an un-upgraded player behaves exactly as before. fireMul is an *interval*
 * multiplier: smaller = faster (legacy CONFIG.upgrades.fireMul = 0.85).
 */
class Mods(
    var gunMul: Float = 1f,
    var fireMul: Float = 1f,
    var meleeMul: Float = 1f,
    var moveMul: Float = 1f,
    var ammoMul: Float = 1f,
    var healOnKill: Float = 0f,
    var wallHp: Float = 70f,
) : Component<Mods> {
    override fun type() = Mods
    companion object : ComponentType<Mods>()
}
