package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.github.panda17tk.arpg.config.EnemyDef

/** A mob: its archetype, scaled speed, per-attack cooldown timers, contact bump timer. */
class Mob(
    val kind: String,
    val def: EnemyDef,
    var speed: Float,
    val attackCd: FloatArray,
    var bumpCd: Float = 0f,
) : Component<Mob> {
    val tier: String get() = def.tier
    override fun type() = Mob
    companion object : ComponentType<Mob>()
}
