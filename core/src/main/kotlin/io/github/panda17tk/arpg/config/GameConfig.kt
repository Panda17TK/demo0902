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
    val upgrades: UpgradesConfig = UpgradesConfig(),
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
        seeRange = 340f, contactKB = 200f, gravityResponse = 0f, // agile — ignores gravity, closes straight in
        dodge = DodgeSpec(0.18f, 0.15f, 2.0f),
        attacks = listOf(
            AttackSpec("blink", cd = 3.0f, maxTiles = 5, dur = 0.1f, minDist = 70f, standoff = 28f),
            AttackSpec("charge_melee", cd = 2.4f, range = 40f, reach = 30f, windup = 0.6f, dmg = 18f, kb = 320f),
        ),
    ),
    // --- Bosses (legacy enemies.js BUILTIN_BOSSES). tier midboss=5 attacks / boss=10. ---
    "brute" to EnemyDef(
        name = "ブルート(中ボス)", tier = "midboss", color = "#d08a3a", hp = 420f, speed = 58f,
        w = 34f, h = 34f, seeRange = 320f, contactKB = 320f, gravityResponse = 1.5f, // heavy — easy to fling into planets
        attacks = listOf(
            AttackSpec("melee", cd = 0.8f, dmg = 16f, range = 22f, arc = 360f),
            AttackSpec("charge_melee", cd = 3.2f, range = 60f, reach = 40f, windup = 0.8f, dmg = 30f, kb = 480f),
            AttackSpec("slam", cd = 4.0f, dmg = 18f, range = 80f, power = 360f),
            AttackSpec("burst", cd = 2.5f, dmg = 8f, count = 5, spread = 40f, speed = 240f),
            AttackSpec("summon", cd = 8.0f, minion = "zombie", count = 2),
        ),
    ),
    "warlock" to EnemyDef(
        name = "ウォーロック(中ボス)", tier = "midboss", color = "#7a5ad0", hp = 360f, speed = 48f,
        w = 30f, h = 30f, seeRange = 360f, contactKB = 240f,
        dodge = DodgeSpec(0.25f, 0.15f, 1.6f),
        attacks = listOf(
            AttackSpec("shot", cd = 1.0f, dmg = 12f, speed = 240f),
            AttackSpec("nova", cd = 5.0f, dmg = 8f, count = 14, speed = 180f),
            AttackSpec("blink", cd = 4.0f, maxTiles = 5, dur = 0.1f, minDist = 80f, standoff = 120f),
            AttackSpec("summon", cd = 7.0f, minion = "spitter", count = 2),
            AttackSpec("heal", cd = 9.0f, amount = 40f),
        ),
    ),
    "overlord" to EnemyDef(
        name = "オーバーロード(ボス)", tier = "boss", color = "#d04a6a", hp = 1200f, speed = 52f,
        w = 46f, h = 46f, seeRange = 480f, contactKB = 360f, gravityResponse = 0.25f, // boss resists being flung
        dodge = DodgeSpec(0.12f, 0.12f, 2.5f),
        attacks = listOf(
            AttackSpec("melee", cd = 0.7f, dmg = 20f, range = 30f, arc = 360f),
            AttackSpec("slam", cd = 3.5f, dmg = 22f, range = 100f, power = 420f),
            AttackSpec("charge", cd = 4.5f, range = 260f, power = 700f),
            AttackSpec("nova", cd = 4.0f, dmg = 10f, count = 20, speed = 200f),
            AttackSpec("burst", cd = 2.0f, dmg = 9f, count = 7, spread = 50f, speed = 260f),
            AttackSpec("barrage", cd = 3.0f, dmg = 9f, count = 9, spread = 90f, speed = 200f),
            AttackSpec("homing", cd = 3.5f, dmg = 12f, speed = 160f, turn = 2f, life = 3.5f),
            AttackSpec("summon", cd = 6.0f, minion = "zombie", count = 3),
            AttackSpec("enrage", cd = 12.0f, mul = 1.6f, duration = 4f),
            AttackSpec("heal", cd = 14.0f, amount = 80f),
        ),
    ),
)
