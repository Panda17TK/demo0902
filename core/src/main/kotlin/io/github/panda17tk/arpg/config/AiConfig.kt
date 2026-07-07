package io.github.panda17tk.arpg.config

import kotlinx.serialization.Serializable

/** AI tuning (legacy CONFIG.ai). */
@Serializable
// v2.99 調整モード: fields are `var` so the tuning popup can turn them live.
data class AiConfig(
    var sepRadius: Float = 26f,
    var hpSlowMul: Float = 0.6f,
    var wanderSlow: Float = 0.35f,
    var wanderStuck: Float = 0.8f,
    var flowRebuildInterval: Float = 0.35f,
    // Mere contact no longer damages the player (only explicit attacks do); dash-rams reuse this knockback.
    var playerKnockback: Float = 260f,
    var iFrameContact: Float = 0.6f,
)
