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
    // v2.36: space drifters coast through the void from world creation. They fight like anyone else
    // but never count toward wave completion / live caps (a far-off drifter can't stall a wave).
    val drifter: Boolean = false,
    // v2.45 賞金首: a named bounty head bursts into this much dust when it falls (0 = not a bounty).
    var bountyDust: Int = 0,
    var bountyName: String = "", // v2.88: the head's name, worn on the boss bar
    var raider: Boolean = false, // v2.110 行商船襲撃: counted by TraderRaidSystem's settlement
    var phase2: Boolean = false, // v2.88: heavies rage past half health (one-way latch)
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
