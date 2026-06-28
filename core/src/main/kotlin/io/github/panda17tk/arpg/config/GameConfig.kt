package io.github.panda17tk.arpg.config

import io.github.panda17tk.arpg.combat.Weapons
import io.github.panda17tk.arpg.combat.WeaponDef
import kotlinx.serialization.Serializable

/** Root editable config. Phase 6b adds upgrades/drops sections. */
@Serializable
data class GameConfig(
    val player: PlayerConfig = PlayerConfig(),
    val weapons: List<WeaponDef> = Weapons.ALL,
    val ai: AiConfig = AiConfig(),
    val waves: WaveConfig = WaveConfig(),
    val enemies: Map<String, EnemyDef> = defaultEnemies(),
)

private fun defaultEnemies(): Map<String, EnemyDef> = mapOf(
    "zombie" to EnemyDef(
        name = "ゾンビ", tier = "normal", color = "#b24a4a", hp = 55f, speed = 72f,
        seeRange = 240f, contactKB = 220f,
        attacks = listOf(
            AttackSpec("melee", cd = 0.9f, dmg = 10f, range = 12f, arc = 360f),
            AttackSpec("lunge", cd = 3.5f, range = 90f, power = 360f),
        ),
    ),
    "spitter" to EnemyDef(
        name = "スピッター", tier = "normal", color = "#3aa06f", hp = 65f, speed = 35f,
        seeRange = 320f, contactKB = 220f,
        attacks = listOf(
            AttackSpec("melee", cd = 3.0f, dmg = 10f, range = 9f, arc = 90f),
            AttackSpec("shot", cd = 1.2f, dmg = 12f, speed = 220f, life = 1.6f),
        ),
    ),
    "stalker" to EnemyDef(
        name = "ストーカー", tier = "normal", color = "#9a6ad0", hp = 60f, speed = 64f,
        seeRange = 340f, contactKB = 200f,
        dodge = DodgeSpec(0.18f, 0.15f, 2.0f),
        attacks = listOf(
            AttackSpec("blink", cd = 3.0f, maxTiles = 5, dur = 0.1f, minDist = 70f, standoff = 28f),
            AttackSpec("charge_melee", cd = 2.4f, range = 40f, reach = 30f, windup = 0.6f, dmg = 18f, kb = 320f),
        ),
    ),
)
