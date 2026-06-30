package io.github.panda17tk.arpg.config

import io.github.panda17tk.arpg.combat.Weapons
import io.github.panda17tk.arpg.combat.WeaponDef
import io.github.panda17tk.arpg.planet.PlanetBiome
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
        bravery = 0.25f, canBeg = true, mercyThreshold = 0.3f, canSpeak = true, lifeKind = LifeKind.SAPIENT, // timid — flees, begs, pleads aloud
        attacks = listOf(
            AttackSpec("melee", cd = 3.0f, dmg = 10f, range = 9f, arc = 90f),
            AttackSpec("shot", cd = 1.2f, dmg = 12f, speed = 220f, life = 1.6f),
        ),
    ),
    "stalker" to EnemyDef(
        name = "ストーカー", tier = "normal", color = "#9a6ad0", hp = 60f, speed = 64f,
        seeRange = 340f, contactKB = 200f, gravityResponse = 0f, // agile — ignores gravity, closes straight in
        intelligence = 0.8f, bravery = 0.3f, canHideAndRest = true, canSpeak = true, lifeKind = LifeKind.SAPIENT, // cunning — hides, rests, taunts
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
    // --- NATURE planet tribe (Living Planets): young, children, a guardian, a shaman and a king. ---
    "young_beast" to EnemyDef(
        name = "幼獣", tier = "normal", color = "#7faf5a", hp = 40f, speed = 78f,
        seeRange = 280f, contactKB = 180f, gravityResponse = 0.8f,
        bravery = 0.3f, biome = PlanetBiome.NATURE, // skittish — flees once wounded
        attacks = listOf(
            AttackSpec("lunge", cd = 3.0f, range = 80f, power = 320f),
            AttackSpec("melee", cd = 0.9f, dmg = 8f, range = 12f, arc = 360f),
        ),
    ),
    "beast_whelp" to EnemyDef(
        name = "獣の子", tier = "normal", color = "#a9d18a", hp = 24f, speed = 86f, w = 16f, h = 16f,
        seeRange = 240f, contactKB = 120f, gravityResponse = 0.6f,
        bravery = 0.1f, familyRole = FamilyRole.CHILD, biome = PlanetBiome.NATURE, // a child — never fights, flees
    ),
    "forest_guardian" to EnemyDef(
        name = "森の守り手", tier = "normal", color = "#4a7f3a", hp = 160f, speed = 56f, w = 28f, h = 28f,
        seeRange = 320f, contactKB = 320f, gravityResponse = 1.4f,
        bravery = 0.9f, protectiveness = 0.95f, familyRole = FamilyRole.GUARDIAN, biome = PlanetBiome.NATURE,
        attacks = listOf(
            AttackSpec("charge_melee", cd = 3.0f, range = 60f, reach = 40f, windup = 0.7f, dmg = 22f, kb = 420f),
            AttackSpec("melee", cd = 0.8f, dmg = 14f, range = 20f, arc = 360f),
        ),
    ),
    "spore_shaman" to EnemyDef(
        name = "胞子の祈祷師", tier = "normal", color = "#6fae8f", hp = 70f, speed = 44f,
        seeRange = 360f, contactKB = 200f, gravityResponse = 0.9f,
        intelligence = 0.85f, bravery = 0.35f, canHideAndRest = true, canSpeak = true, lifeKind = LifeKind.SAPIENT, biome = PlanetBiome.NATURE,
        attacks = listOf(
            AttackSpec("shot", cd = 1.1f, dmg = 11f, speed = 220f, life = 1.6f),
            AttackSpec("heal", cd = 9.0f, amount = 30f),
        ),
    ),
    "beast_king" to EnemyDef(
        name = "獣王", tier = "midboss", color = "#2f6f2a", hp = 480f, speed = 54f, w = 36f, h = 36f,
        seeRange = 360f, contactKB = 360f, gravityResponse = 0.6f,
        protectiveness = 0.7f, canSpeak = true, lifeKind = LifeKind.SAPIENT, familyRole = FamilyRole.KING, biome = PlanetBiome.NATURE,
        attacks = listOf(
            AttackSpec("melee", cd = 0.8f, dmg = 18f, range = 26f, arc = 360f),
            AttackSpec("summon", cd = 8.0f, minion = "young_beast", count = 3),
            AttackSpec("slam", cd = 4.0f, dmg = 18f, range = 80f, power = 360f),
        ),
    ),
    // --- MAGMA planet: aggressive imps, lava throwers, an obsidian guard and a volcano king. ---
    "ember_imp" to EnemyDef(
        name = "燼の小鬼", tier = "normal", color = "#e0623a", hp = 30f, speed = 82f,
        seeRange = 300f, contactKB = 180f, gravityResponse = 0.7f,
        bravery = 0.8f, biome = PlanetBiome.MAGMA, // weak but reckless — barely flees
        attacks = listOf(
            AttackSpec("shot", cd = 1.0f, dmg = 9f, speed = 240f, life = 1.4f),
            AttackSpec("melee", cd = 0.9f, dmg = 9f, range = 12f, arc = 360f),
        ),
    ),
    "lava_thrower" to EnemyDef(
        name = "溶岩投げ", tier = "normal", color = "#c2502a", hp = 78f, speed = 40f,
        seeRange = 360f, contactKB = 220f, gravityResponse = 1.0f,
        intelligence = 0.45f, bravery = 0.6f, biome = PlanetBiome.MAGMA, // ranged — keeps its distance
        attacks = listOf(
            AttackSpec("shot", cd = 1.3f, dmg = 13f, speed = 200f, life = 1.8f),
            AttackSpec("burst", cd = 3.2f, dmg = 8f, count = 4, spread = 36f, speed = 200f),
        ),
    ),
    "obsidian_guard" to EnemyDef(
        name = "黒曜の守り", tier = "normal", color = "#5a3a30", hp = 210f, speed = 50f, w = 28f, h = 28f,
        seeRange = 320f, contactKB = 340f, gravityResponse = 1.5f,
        bravery = 0.95f, protectiveness = 0.85f, familyRole = FamilyRole.GUARDIAN, biome = PlanetBiome.MAGMA,
        attacks = listOf(
            AttackSpec("charge_melee", cd = 3.0f, range = 60f, reach = 42f, windup = 0.8f, dmg = 24f, kb = 460f),
            AttackSpec("guard", cd = 9.0f, mul = 0.5f, duration = 3.5f),
            AttackSpec("melee", cd = 0.8f, dmg = 16f, range = 22f, arc = 360f),
        ),
    ),
    "volcano_king" to EnemyDef(
        name = "火山王", tier = "midboss", color = "#a83218", hp = 540f, speed = 50f, w = 38f, h = 38f,
        seeRange = 380f, contactKB = 360f, gravityResponse = 0.6f,
        protectiveness = 0.7f, canSpeak = true, lifeKind = LifeKind.SAPIENT, familyRole = FamilyRole.KING, biome = PlanetBiome.MAGMA,
        attacks = listOf(
            AttackSpec("melee", cd = 0.8f, dmg = 20f, range = 28f, arc = 360f),
            AttackSpec("nova", cd = 5.0f, dmg = 10f, count = 16, speed = 200f),
            AttackSpec("burst", cd = 2.6f, dmg = 9f, count = 6, spread = 44f, speed = 240f),
            AttackSpec("summon", cd = 8.0f, minion = "ember_imp", count = 3),
        ),
    ),
    // --- ICE planet: timid frostlings, spearmen, a cunning stalker and an ice queen. ---
    "frostling" to EnemyDef(
        name = "氷の小妖", tier = "normal", color = "#bfe6f0", hp = 28f, speed = 86f,
        seeRange = 260f, contactKB = 160f, gravityResponse = 0.8f,
        bravery = 0.15f, canBeg = true, mercyThreshold = 0.3f, canSpeak = true, lifeKind = LifeKind.SAPIENT, biome = PlanetBiome.ICE, // timid — flees and pleads
        attacks = listOf(
            AttackSpec("lunge", cd = 3.2f, range = 70f, power = 280f),
            AttackSpec("melee", cd = 1.0f, dmg = 8f, range = 12f, arc = 360f),
        ),
    ),
    "ice_spearman" to EnemyDef(
        name = "氷槍兵", tier = "normal", color = "#7fb8d8", hp = 62f, speed = 52f,
        seeRange = 360f, contactKB = 220f, gravityResponse = 1.0f,
        intelligence = 0.5f, bravery = 0.55f, biome = PlanetBiome.ICE, // keeps distance on the snowfields
        attacks = listOf(
            AttackSpec("shot", cd = 1.2f, dmg = 12f, speed = 230f, life = 1.7f),
        ),
    ),
    "snow_stalker" to EnemyDef(
        name = "雪の追跡者", tier = "normal", color = "#9ad0e6", hp = 64f, speed = 66f,
        seeRange = 340f, contactKB = 200f, gravityResponse = 0f,
        intelligence = 0.85f, bravery = 0.3f, canHideAndRest = true, canSpeak = true, lifeKind = LifeKind.SAPIENT, biome = PlanetBiome.ICE,
        dodge = DodgeSpec(0.18f, 0.15f, 2.0f),
        attacks = listOf(
            AttackSpec("blink", cd = 3.0f, maxTiles = 5, dur = 0.1f, minDist = 70f, standoff = 28f),
            AttackSpec("charge_melee", cd = 2.4f, range = 40f, reach = 30f, windup = 0.6f, dmg = 18f, kb = 320f),
        ),
    ),
    "ice_queen" to EnemyDef(
        name = "氷の女王", tier = "midboss", color = "#5fa6d8", hp = 500f, speed = 48f, w = 36f, h = 36f,
        seeRange = 380f, contactKB = 340f, gravityResponse = 0.6f,
        protectiveness = 0.7f, canSpeak = true, lifeKind = LifeKind.SAPIENT, familyRole = FamilyRole.KING, biome = PlanetBiome.ICE,
        attacks = listOf(
            AttackSpec("nova", cd = 4.6f, dmg = 9f, count = 18, speed = 190f),
            AttackSpec("summon", cd = 8.0f, minion = "frostling", count = 3),
            AttackSpec("guard", cd = 10.0f, mul = 0.5f, duration = 4f),
            AttackSpec("shot", cd = 1.2f, dmg = 12f, speed = 220f),
        ),
    ),
    // --- GAS planet: weightless drifters that ignore gravity (gravityResponse = 0). ---
    "wind_jelly" to EnemyDef(
        name = "風クラゲ", tier = "normal", color = "#cdbff0", hp = 46f, speed = 38f,
        seeRange = 300f, contactKB = 160f, gravityResponse = 0f, // weightless drifter
        bravery = 0.5f, biome = PlanetBiome.GAS,
        dodge = DodgeSpec(0.3f, 0.15f, 1.6f),
        attacks = listOf(
            AttackSpec("melee", cd = 1.0f, dmg = 10f, range = 14f, arc = 360f),
        ),
    ),
    "storm_orb" to EnemyDef(
        name = "嵐の球", tier = "normal", color = "#9fa8e0", hp = 36f, speed = 30f,
        seeRange = 340f, contactKB = 180f, gravityResponse = 0f, // floating trap
        intelligence = 0.3f, bravery = 0.7f, biome = PlanetBiome.GAS,
        attacks = listOf(
            AttackSpec("homing", cd = 3.4f, dmg = 11f, speed = 150f, turn = 2f, life = 3.5f),
            AttackSpec("mine", cd = 5.0f, dmg = 14f, life = 6f),
        ),
    ),
    "gravity_wraith" to EnemyDef(
        name = "重力の亡霊", tier = "normal", color = "#b0a0e8", hp = 70f, speed = 60f,
        seeRange = 360f, contactKB = 200f, gravityResponse = 0f,
        intelligence = 0.8f, bravery = 0.3f, canHideAndRest = true, canSpeak = true, lifeKind = LifeKind.SAPIENT, biome = PlanetBiome.GAS,
        dodge = DodgeSpec(0.2f, 0.15f, 1.8f),
        attacks = listOf(
            AttackSpec("blink", cd = 3.2f, maxTiles = 6, dur = 0.1f, minDist = 80f, standoff = 60f),
            AttackSpec("homing", cd = 2.8f, dmg = 10f, speed = 160f, turn = 2.4f, life = 3.2f),
        ),
    ),
    "storm_core" to EnemyDef(
        name = "嵐の核(ボス)", tier = "boss", color = "#7a78d8", hp = 1080f, speed = 40f, w = 44f, h = 44f,
        seeRange = 460f, contactKB = 320f, gravityResponse = 0f, // the eye of the storm — unmoved by gravity
        canSpeak = true, lifeKind = LifeKind.SAPIENT, biome = PlanetBiome.GAS,
        attacks = listOf(
            AttackSpec("nova", cd = 4.0f, dmg = 10f, count = 20, speed = 200f),
            AttackSpec("barrage", cd = 3.0f, dmg = 9f, count = 9, spread = 90f, speed = 200f),
            AttackSpec("summon", cd = 6.5f, minion = "storm_orb", count = 3),
            AttackSpec("homing", cd = 3.5f, dmg = 12f, speed = 160f, turn = 2f, life = 3.5f),
        ),
    ),
    // --- DEAD planet: emotionless husks, a ruin guard and a talking oracle among the ruins. ---
    "bone_drone" to EnemyDef(
        name = "骨の遊機", tier = "normal", color = "#9a958c", hp = 60f, speed = 60f,
        seeRange = 300f, contactKB = 200f, gravityResponse = 1.0f,
        biome = PlanetBiome.DEAD, // emotionless husk — never begs, never flees (legacy-hostile defaults)
        attacks = listOf(
            AttackSpec("shot", cd = 1.4f, dmg = 10f, speed = 210f, life = 1.6f),
            AttackSpec("melee", cd = 0.9f, dmg = 12f, range = 14f, arc = 360f),
        ),
    ),
    "ruin_guard" to EnemyDef(
        name = "遺跡の守番", tier = "normal", color = "#7d7a72", hp = 250f, speed = 40f, w = 30f, h = 30f,
        seeRange = 300f, contactKB = 360f, gravityResponse = 1.6f,
        bravery = 1f, biome = PlanetBiome.DEAD, // tough sentinel
        attacks = listOf(
            AttackSpec("charge_melee", cd = 3.2f, range = 60f, reach = 44f, windup = 0.85f, dmg = 26f, kb = 480f),
            AttackSpec("guard", cd = 10.0f, mul = 0.5f, duration = 4f),
            AttackSpec("melee", cd = 0.8f, dmg = 16f, range = 22f, arc = 360f),
        ),
    ),
    "dead_oracle" to EnemyDef(
        name = "死の託宣者", tier = "midboss", color = "#8c86b0", hp = 320f, speed = 44f, w = 32f, h = 32f,
        seeRange = 400f, contactKB = 220f, gravityResponse = 0.7f,
        intelligence = 0.7f, bravery = 0.6f, canHideAndRest = true, canSpeak = true, lifeKind = LifeKind.SAPIENT, biome = PlanetBiome.DEAD,
        attacks = listOf(
            AttackSpec("shot", cd = 1.1f, dmg = 12f, speed = 220f, life = 1.8f),
            AttackSpec("nova", cd = 5.5f, dmg = 9f, count = 14, speed = 180f),
            AttackSpec("heal", cd = 10.0f, amount = 36f),
        ),
    ),
    // --- LONELY asteroid: an event encounter — a lone soldier, an exiled king, a peaceful monk. ---
    "lost_soldier" to EnemyDef(
        name = "はぐれ兵", tier = "midboss", color = "#b9a06a", hp = 180f, speed = 60f, w = 26f, h = 26f,
        seeRange = 360f, contactKB = 240f, gravityResponse = 1.0f,
        intelligence = 0.55f, bravery = 0.3f, canBeg = true, mercyThreshold = 0.4f, canSpeak = true, lifeKind = LifeKind.SAPIENT, biome = PlanetBiome.LONELY,
        attacks = listOf(
            AttackSpec("shot", cd = 1.2f, dmg = 12f, speed = 230f, life = 1.7f),
            AttackSpec("charge_melee", cd = 2.8f, range = 50f, reach = 34f, windup = 0.7f, dmg = 18f, kb = 340f),
            AttackSpec("melee", cd = 0.8f, dmg = 12f, range = 16f, arc = 360f),
        ),
    ),
    "exiled_king" to EnemyDef(
        name = "追放王(ボス)", tier = "boss", color = "#c8a23a", hp = 760f, speed = 54f, w = 42f, h = 42f,
        seeRange = 440f, contactKB = 380f, gravityResponse = 0.4f,
        bravery = 1f, protectiveness = 0.4f, canSpeak = true, lifeKind = LifeKind.SAPIENT, familyRole = FamilyRole.KING, biome = PlanetBiome.LONELY,
        attacks = listOf(
            AttackSpec("melee", cd = 0.7f, dmg = 22f, range = 30f, arc = 360f),
            AttackSpec("charge", cd = 4.5f, range = 260f, power = 680f),
            AttackSpec("nova", cd = 4.2f, dmg = 11f, count = 18, speed = 200f),
            AttackSpec("slam", cd = 3.6f, dmg = 22f, range = 100f, power = 420f),
        ),
    ),
    "star_monk" to EnemyDef(
        name = "星の修道士", tier = "normal", color = "#d8d0a0", hp = 90f, speed = 72f,
        seeRange = 320f, contactKB = 160f, gravityResponse = 0.6f,
        intelligence = 0.6f, bravery = 0.1f, canSpeak = true, lifeKind = LifeKind.SAPIENT, biome = PlanetBiome.LONELY, // peaceful — flees if attacked
    ),

    // ===== Wildlife (LifeKind.WILDLIFE): mute animals that build an ecosystem, not a society. =====
    // --- NATURE: a whole food web — skittish prey, a grazing herd, pack hunters, a guarded nest, a lone apex. ---
    "moss_hopper" to EnemyDef(
        name = "コケトビ", tier = "normal", color = "#8fcf6f", hp = 24f, speed = 104f, w = 16f, h = 16f,
        seeRange = 260f, contactKB = 120f, gravityResponse = 0.8f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.PREY, diet = Diet.HERBIVORE, herdAffinity = 0.6f, fear = 0.95f, biome = PlanetBiome.NATURE,
    ),
    "horn_deer" to EnemyDef(
        name = "ツノジカ", tier = "normal", color = "#b9925a", hp = 78f, speed = 82f, w = 24f, h = 24f,
        seeRange = 320f, contactKB = 200f, gravityResponse = 1.0f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.HERD, diet = Diet.HERBIVORE, herdAffinity = 0.9f, fear = 0.6f, biome = PlanetBiome.NATURE,
    ),
    "fang_wolf" to EnemyDef(
        name = "牙オオカミ", tier = "normal", color = "#7a7f88", hp = 62f, speed = 96f, w = 22f, h = 20f,
        seeRange = 360f, contactKB = 240f, gravityResponse = 0.9f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.PREDATOR, diet = Diet.CARNIVORE, herdAffinity = 0.45f, fear = 0.25f, biome = PlanetBiome.NATURE,
        attacks = listOf(
            AttackSpec("lunge", cd = 3.0f, range = 110f, power = 380f),
            AttackSpec("melee", cd = 0.9f, dmg = 12f, range = 16f, arc = 360f),
        ),
    ),
    "root_boar" to EnemyDef(
        name = "ねもといのしし", tier = "normal", color = "#9a6b4a", hp = 120f, speed = 66f, w = 28f, h = 24f,
        seeRange = 260f, contactKB = 280f, gravityResponse = 1.1f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.NEST_GUARD, diet = Diet.HERBIVORE, herdAffinity = 0.3f, fear = 0.2f, territoryRadius = 150f, biome = PlanetBiome.NATURE,
        attacks = listOf(
            AttackSpec("charge_melee", cd = 3.2f, range = 70f, reach = 30f, windup = 0.6f, dmg = 16f, kb = 360f),
        ),
    ),
    "nest_mother" to EnemyDef(
        name = "巣の母", tier = "normal", color = "#6f9a52", hp = 170f, speed = 50f, w = 32f, h = 28f,
        seeRange = 320f, contactKB = 300f, gravityResponse = 0.9f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.NEST_GUARD, diet = Diet.OMNIVORE, herdAffinity = 0.3f, fear = 0.1f, territoryRadius = 180f, biome = PlanetBiome.NATURE,
        attacks = listOf(
            AttackSpec("melee", cd = 0.9f, dmg = 14f, range = 22f, arc = 360f),
            AttackSpec("charge_melee", cd = 3.6f, range = 80f, reach = 34f, windup = 0.7f, dmg = 18f, kb = 380f),
        ),
    ),
    "forest_hatchling" to EnemyDef(
        name = "孵りたて", tier = "normal", color = "#bfe08f", hp = 18f, speed = 84f, w = 14f, h = 14f,
        seeRange = 220f, contactKB = 90f, gravityResponse = 0.8f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.HATCHLING, diet = Diet.OMNIVORE, herdAffinity = 0.7f, fear = 1.0f, biome = PlanetBiome.NATURE,
    ),
    "forest_apex" to EnemyDef(
        name = "森の主", tier = "normal", color = "#3f6f3a", hp = 300f, speed = 74f, w = 34f, h = 30f,
        seeRange = 400f, contactKB = 340f, gravityResponse = 0.7f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.APEX, diet = Diet.CARNIVORE, herdAffinity = 0.1f, fear = 0.05f, territoryRadius = 240f, biome = PlanetBiome.NATURE,
        attacks = listOf(
            AttackSpec("lunge", cd = 3.4f, range = 140f, power = 460f),
            AttackSpec("charge_melee", cd = 3.4f, range = 90f, reach = 38f, windup = 0.7f, dmg = 22f, kb = 420f),
            AttackSpec("melee", cd = 0.8f, dmg = 18f, range = 26f, arc = 360f),
        ),
    ),
    // --- Other biomes: a light touch for now (one wild species each). ---
    "ember_moth" to EnemyDef(
        name = "燻り蛾", tier = "normal", color = "#e08a4a", hp = 22f, speed = 108f, w = 14f, h = 14f,
        seeRange = 260f, contactKB = 110f, gravityResponse = 0.5f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.PREY, diet = Diet.ENERGY, herdAffinity = 0.5f, fear = 0.9f, biome = PlanetBiome.MAGMA,
    ),
    "frost_hare" to EnemyDef(
        name = "霜ウサギ", tier = "normal", color = "#bfe6ef", hp = 44f, speed = 92f, w = 18f, h = 16f,
        seeRange = 300f, contactKB = 150f, gravityResponse = 1.0f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.HERD, diet = Diet.HERBIVORE, herdAffinity = 0.85f, fear = 0.85f, biome = PlanetBiome.ICE,
    ),
)
