package io.github.panda17tk.arpg.config

import kotlinx.serialization.Serializable

/**
 * Magnitudes for the between-wave upgrade cards (legacy CONFIG.upgrades). Editable so the
 * dev/balance pass can tune effect strength without touching the upgrade logic.
 */
@Serializable
data class UpgradesConfig(
    val gunMul: Float = 1.25f,    // 火力強化 (multiplicative)
    val fireMul: Float = 0.85f,   // 連射強化 (fire-interval multiplier — smaller is faster)
    val meleeMul: Float = 1.35f,  // 近接強化
    val maxHpAdd: Float = 25f,    // 頑強 (additive, then full heal)
    val moveMul: Float = 1.12f,   // 俊足
    val ammoMul: Float = 1.5f,    // 弾薬調達 (future drop-rate multiplier)
    val lifestealAdd: Float = 2f, // 吸血 (HP per kill, additive)
    val wallHpAdd: Float = 40f,   // 築城術 (placed-wall HP, additive)
    val blocksAdd: Int = 4,       // 築城術 (materials)
    // Flat ammo granted immediately by 弾薬調達 (legacy upgrades.js apply()).
    val ammoRefill9: Int = 40,
    val ammoRefill12: Int = 8,
    val ammoRefillBeam: Int = 2,
    val ammoRefillNade: Int = 1,
)
