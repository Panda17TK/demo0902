package io.github.panda17tk.arpg.config

import kotlinx.serialization.Serializable

/** Wave/spawner tuning (legacy CONFIG.waves). */
@Serializable
data class WaveConfig(
    val firstWaveDelay: Float = 3.0f,
    val intermission: Float = 4.0f,
    val baseQuota: Int = 6,
    val quotaPerWave: Int = 3,
    val maxQuota: Int = 40,
    val hpScalePerWave: Float = 0.12f,
    val speedScalePerWave: Float = 0.04f,
    val liveCapBase: Int = 8,
    val liveCapPerWave: Int = 2,
    val maxLiveCap: Int = 28,
    val spawnIntervalBase: Float = 0.9f,
    val spawnIntervalPerWave: Float = 0.04f,
    val minSpawnInterval: Float = 0.25f,
    val midBossEvery: Int = 5,
    val bossEvery: Int = 10,
)
