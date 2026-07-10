package io.github.panda17tk.arpg.config

import io.github.panda17tk.arpg.combat.Weapons
import io.github.panda17tk.arpg.combat.WeaponDef
import kotlinx.serialization.Serializable

/** Root editable config. Phase 6b adds upgrades/drops sections. The enemy roster lives in DefaultEnemies.kt. */
@Serializable
data class GameConfig(
    val player: PlayerConfig = PlayerConfig(),
    val weapons: List<WeaponDef> = Weapons.ALL,
    val ai: AiConfig = AiConfig(),
    val wild: WildConfig = WildConfig(), // v2.143 調整モード・野生の棚
    val waves: WaveConfig = WaveConfig(),
    val upgrades: UpgradesConfig = UpgradesConfig(),
    val enemies: Map<String, EnemyDef> = defaultEnemies(),
)
