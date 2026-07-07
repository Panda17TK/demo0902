package io.github.panda17tk.arpg.config

import kotlinx.serialization.Serializable

/** Editable player/combat balance. Defaults mirror the legacy CONFIG.player.
 *  v2.98 調整モード: fields are `var` so the in-game tuning popup can turn them live. */
@Serializable
data class PlayerConfig(
    var baseSpeed: Float = 85f,
    var speedMul: Float = 1.2f,
    var dashMul: Float = 2.5f,
    var hpMax: Float = 100f,
    var staMax: Float = 300f,
    var staDrain: Float = 35f,
    var staRegen: Float = 44f,
    var meleeDmg: Float = 22f,
    var meleeReach: Float = 51f,
    var meleeCd: Float = 0.32f,
    var meleeSlashDmg: Float = 8f,
    var meleeStaWeakBelow: Float = 0.40f,
    var meleeStaSwordMin: Float = 0.20f,
    var meleeWeakMul: Float = 0.6f,
    var fistDmg: Float = 8f,
    var meleeStaCost: Float = 18f,   // stamina drained per melee swing
    var dashThrust: Float = 760f,    // space dash inertia: accel added to drift while dashing (stronger glide)
    var bulletSpeed: Float = 360f,
    var bulletLife: Float = 1.8f,    // doubled (was 0.9) → gun range ×2 for pistol/shotgun/MG
    var grenadeSpeed: Float = 168f, // v2.80: 60% of the old 280 — a heavier, deliberate arc
    var grenadeFuse: Float = 1.0f,
    var autoReloadDelay: Float = 0.8f,
    var explodeRadius: Float = 140f, // v2.80: twice the old 70 — the blast owns the room
    var explodeDmg: Float = 110f,
    var explodeSelfDmg: Float = 25f,
    var explodeWallDmg: Float = 120f,
)
