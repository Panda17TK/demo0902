package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.github.panda17tk.arpg.config.EnemyDef
import io.github.panda17tk.arpg.config.WildState

/** A mob: its archetype, scaled speed, per-attack cooldown timers, contact bump timer. */
class Mob(
    val kind: String,
    val def: EnemyDef,
    var speed: Float,
    val attackCd: FloatArray,
    val waveNum: Int = 1,
    var bumpCd: Float = 0f,
    var tribe: Int = 0,
    var level: Int = 1,
    var xp: Float = 0f,
    val dashes: Boolean = false, // ~half of each tribe's rank-and-file dash (facing-thrust inertial burst)
    var dashCd: Float = 0f,
    // Wildlife runtime (WildlifeSystem / WildPredationSystem only; ignored by hostile/sapient mobs).
    var wildState: WildState = WildState.Wander,
    var hunger: Float = 0f,   // 0..1, climbs over time; a predator hunts when high, drops while feeding
    var wanderCd: Float = 0f, // re-roll timer for the idle wander heading
    var feedCd: Float = 0f,   // cooldown between predation bites
    var homeX: Float = 0f,    // nest / home position (set to the spawn point); nest-guards return here
    var homeY: Float = 0f,
) : Component<Mob> {
    val tier: String get() = def.tier
    override fun type() = Mob
    companion object : ComponentType<Mob>()
}
