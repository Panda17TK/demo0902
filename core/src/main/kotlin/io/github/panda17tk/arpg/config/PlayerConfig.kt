package io.github.panda17tk.arpg.config

import kotlinx.serialization.Serializable

/** Editable player/combat balance. Defaults mirror the legacy CONFIG.player. */
@Serializable
data class PlayerConfig(
    val baseSpeed: Float = 110f,
    val speedMul: Float = 1.2f,
    val dashMul: Float = 2f,
    val hpMax: Float = 100f,
    val staMax: Float = 100f,
    val staDrain: Float = 35f,
    val staRegen: Float = 22f,
    val meleeDmg: Float = 22f,
    val meleeReach: Float = 51f,
    val meleeCd: Float = 0.32f,
    val meleeSlashDmg: Float = 8f,
    val meleeStaWeakBelow: Float = 0.40f,
    val meleeStaSwordMin: Float = 0.20f,
    val meleeWeakMul: Float = 0.6f,
    val fistDmg: Float = 8f,
    val meleeStaCost: Float = 18f,   // stamina drained per melee swing
    val dashThrust: Float = 520f,    // space dash inertia: accel added to drift while dashing
    val bulletSpeed: Float = 360f,
    val bulletLife: Float = 0.9f,
    val grenadeSpeed: Float = 280f,
    val grenadeFuse: Float = 1.0f,
    val autoReloadDelay: Float = 0.8f,
    val explodeRadius: Float = 70f,
    val explodeDmg: Float = 110f,
    val explodeSelfDmg: Float = 25f,
    val explodeWallDmg: Float = 120f,
)
