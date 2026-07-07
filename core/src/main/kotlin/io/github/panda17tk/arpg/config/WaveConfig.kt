package io.github.panda17tk.arpg.config

import kotlinx.serialization.Serializable

/** Wave/spawner tuning (legacy CONFIG.waves). */
@Serializable
// v2.99 調整モード: fields are `var` so the tuning popup can turn them live.
data class WaveConfig(
    var firstWaveDelay: Float = 3.0f,
    var intermission: Float = 4.0f,
    var baseQuota: Int = 20,      // v2.32: さらに増量 (14 → 20)
    var quotaPerWave: Int = 7,    // v2.32: 5 → 7
    var maxQuota: Int = 130,      // v2.32: 90 → 130
    var hpScalePerWave: Float = 0.12f,
    var speedScalePerWave: Float = 0.04f,
    var liveCapBase: Int = 26,    // v2.32: 18 → 26 (同時生存数も引き上げ)
    var liveCapPerWave: Int = 5,  // v2.32: 4 → 5
    var maxLiveCap: Int = 85,     // v2.32: 60 → 85
    var spawnIntervalBase: Float = 0.9f,
    var spawnIntervalPerWave: Float = 0.04f,
    var minSpawnInterval: Float = 0.25f,
    var midBossEvery: Int = 5,
    var bossEvery: Int = 10,
)
