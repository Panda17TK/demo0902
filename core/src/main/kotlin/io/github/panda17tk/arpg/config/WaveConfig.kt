package io.github.panda17tk.arpg.config

import kotlinx.serialization.Serializable

/** Wave/spawner tuning (legacy CONFIG.waves). */
@Serializable
data class WaveConfig(
    val firstWaveDelay: Float = 3.0f,
    val intermission: Float = 4.0f,
    val baseQuota: Int = 20,      // v2.32: さらに増量 (14 → 20)
    val quotaPerWave: Int = 7,    // v2.32: 5 → 7
    val maxQuota: Int = 130,      // v2.32: 90 → 130
    val hpScalePerWave: Float = 0.12f,
    val speedScalePerWave: Float = 0.04f,
    val liveCapBase: Int = 26,    // v2.32: 18 → 26 (同時生存数も引き上げ)
    val liveCapPerWave: Int = 5,  // v2.32: 4 → 5
    val maxLiveCap: Int = 85,     // v2.32: 60 → 85
    val spawnIntervalBase: Float = 0.9f,
    val spawnIntervalPerWave: Float = 0.04f,
    val minSpawnInterval: Float = 0.25f,
    val midBossEvery: Int = 5,
    val bossEvery: Int = 10,
)
