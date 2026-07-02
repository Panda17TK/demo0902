package io.github.panda17tk.arpg.config

import kotlinx.serialization.Serializable

/** AI tuning (legacy CONFIG.ai). */
@Serializable
data class AiConfig(
    val sepRadius: Float = 26f,
    val hpSlowMul: Float = 0.6f,
    val wanderSlow: Float = 0.35f,
    val wanderStuck: Float = 0.8f,
    val flowRebuildInterval: Float = 0.35f,
    // Mere contact no longer damages the player (only explicit attacks do); dash-rams reuse this knockback.
    val playerKnockback: Float = 260f,
    val iFrameContact: Float = 0.6f,
)
