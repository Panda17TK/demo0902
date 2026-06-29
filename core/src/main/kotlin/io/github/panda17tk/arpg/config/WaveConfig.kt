package io.github.panda17tk.arpg.config

import kotlinx.serialization.Serializable

/** Wave/spawner tuning (legacy CONFIG.waves). */
@Serializable
data class WaveConfig(
    val firstWaveDelay: Float = 3.0f,
    val intermission: Float = 4.0f,
    val baseQuota: Int = 14,
    val quotaPerWave: Int = 5,
    val maxQuota: Int = 90,
    val hpScalePerWave: Float = 0.12f,
    val speedScalePerWave: Float = 0.04f,
    val liveCapBase: Int = 18,
    val liveCapPerWave: Int = 4,
    val maxLiveCap: Int = 60,
    val spawnIntervalBase: Float = 0.9f,
    val spawnIntervalPerWave: Float = 0.04f,
    val minSpawnInterval: Float = 0.25f,
    val midBossEvery: Int = 5,
    val bossEvery: Int = 10,
)
