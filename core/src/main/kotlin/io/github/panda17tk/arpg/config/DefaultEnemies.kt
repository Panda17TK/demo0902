package io.github.panda17tk.arpg.config

import io.github.panda17tk.arpg.planet.PlanetBiome

/**
 * The built-in enemy roster (legacy enemies.js), grouped by home: the space waves + bosses,
 * then each planet's society (Living Planets sapients) and its wildlife food web.
 *
 * NOTE: insertion order is part of the contract — DesyncSurgeSystem/BaseSystem snapshot filtered
 * key lists and index them with the seeded RNG, so reordering entries changes deterministic
 * spawns. New enemies belong at the end of their section.
 */
internal fun defaultEnemies(): Map<String, EnemyDef> = buildMap {
    putAll(spaceEnemies())
    putAll(spaceBosses())
    putAll(natureSociety())
    putAll(magmaSociety())
    putAll(iceSociety())
    putAll(gasSociety())
    putAll(deadSociety())
    putAll(lonelyEncounters())
    putAll(natureWildlife())
    putAll(pioneerWildlife())
    putAll(magmaWildlife())
    putAll(iceWildlife())
    putAll(gasWildlife())
    putAll(deadWildlife())
    putAll(lonelyWildlife())
    // v2.82 大増員: appended LAST on purpose — the surge pool and midboss rotation grow at the
    // tail, so every wave the old versions produced stays byte-identical (determinism contract).
    putAll(expansionSpace())
    putAll(rogueDrifter()) // v2.83
    putAll(expansionSociety())
    putAll(expansionWildlife())
}

/** The space waves' rank-and-file: zombie / spitter / stalker (legacy BUILTIN_ENEMIES). */
private fun spaceEnemies(): Map<String, EnemyDef> = mapOf(
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
    // --- v2.39 roster expansion: varied attacks / silhouettes / sizes / temperaments / smarts. ---
    "lancer" to EnemyDef(
        name = "ランサー", tier = "normal", color = "#c8b25a", hp = 50f, speed = 88f,
        w = 24f, h = 12f, seeRange = 380f, contactKB = 260f, // long, flat silhouette — a living spear
        bravery = 0.95f, intelligence = 0.15f, // fearless and single-minded: it only knows the charge
        attacks = listOf(
            AttackSpec("charge_melee", cd = 2.0f, range = 120f, reach = 34f, windup = 0.5f, dmg = 16f, kb = 380f),
        ),
    ),
    "mortar_bug" to EnemyDef(
        name = "モーター蟲", tier = "normal", color = "#6a8f3a", hp = 45f, speed = 26f,
        w = 18f, h = 22f, seeRange = 420f, contactKB = 180f, // squat lobber, hangs far back
        bravery = 0.15f, canBeg = true, mercyThreshold = 0.4f, canSpeak = true, lifeKind = LifeKind.SAPIENT, // a coward with a cannon
        attacks = listOf(
            AttackSpec("shot", cd = 2.6f, dmg = 22f, speed = 150f, life = 3.2f), // slow, heavy shells
        ),
    ),
    "shield_bearer" to EnemyDef(
        name = "盾持ち", tier = "normal", color = "#5a7a9a", hp = 130f, speed = 34f,
        w = 28f, h = 28f, seeRange = 260f, contactKB = 340f, // a walking wall
        bravery = 0.85f, protectiveness = 0.9f, intelligence = 0.5f, canSpeak = true, lifeKind = LifeKind.SAPIENT, // stands its ground, shields the line
        attacks = listOf(
            AttackSpec("guard", cd = 6.0f, duration = 2.5f, mul = 0.3f),
            AttackSpec("slam", cd = 3.5f, dmg = 14f, range = 60f, power = 320f),
        ),
    ),
    "mite" to EnemyDef(
        name = "マイト", tier = "normal", color = "#d0d05a", hp = 14f, speed = 105f,
        w = 9f, h = 9f, seeRange = 300f, contactKB = 90f, // a fingernail-sized biter
        bravery = 1f, intelligence = 0.05f, // no mind at all — just teeth and momentum
        attacks = listOf(
            AttackSpec("melee", cd = 0.7f, dmg = 4f, range = 8f, arc = 360f),
        ),
    ),
    "marksman" to EnemyDef(
        name = "マークスマン", tier = "normal", color = "#8fa0b8", hp = 40f, speed = 46f,
        w = 14f, h = 24f, seeRange = 560f, contactKB = 200f, // tall, thin, sees further than anything
        intelligence = 0.9f, bravery = 0.25f, canHideAndRest = true, canSpeak = true, lifeKind = LifeKind.SAPIENT, // patient — hides, waits, lines up the shot
        dodge = DodgeSpec(0.12f, 0.2f, 2.5f),
        attacks = listOf(
            AttackSpec("shot", cd = 3.0f, dmg = 30f, speed = 420f, life = 2.0f), // one sharp crack
        ),
    ),
    "sapper" to EnemyDef(
        name = "サッパー", tier = "normal", color = "#b06a9a", hp = 55f, speed = 58f,
        w = 16f, h = 16f, seeRange = 320f, contactKB = 220f,
        intelligence = 0.7f, bravery = 0.45f, canSpeak = true, lifeKind = LifeKind.SAPIENT, // an engineer: seeds the field and withdraws
        attacks = listOf(
            AttackSpec("mine", cd = 3.5f, dmg = 20f, life = 9f),
            AttackSpec("melee", cd = 1.4f, dmg = 8f, range = 10f, arc = 180f),
        ),
    ),
    // --- v2.41: enemies built around the new attack types (spiral / spray / twin_shot / shockwave). ---
    "prism" to EnemyDef(
        name = "プリズム", tier = "normal", color = "#8d82c4", hp = 80f, speed = 24f,
        w = 20f, h = 20f, seeRange = 300f, contactKB = 200f, gravityResponse = 0.4f,
        bravery = 0.6f, intelligence = 0.2f, // a crystal organism — it doesn't think, it refracts
        attacks = listOf(
            AttackSpec("spiral", cd = 4.0f, dmg = 7f, count = 10, speed = 160f, life = 2.2f),
        ),
    ),
    "sprayer" to EnemyDef(
        name = "スプレイヤー", tier = "normal", color = "#7aa06a", hp = 50f, speed = 40f,
        w = 17f, h = 15f, seeRange = 340f, contactKB = 200f,
        bravery = 0.5f, intelligence = 0.3f, canSpeak = true, lifeKind = LifeKind.SAPIENT, // sprays first, thinks later
        attacks = listOf(
            AttackSpec("spray", cd = 2.2f, dmg = 6f, count = 6, spread = 70f, speed = 210f, life = 1.4f),
        ),
    ),
    "duelist" to EnemyDef(
        name = "デュエリスト", tier = "normal", color = "#d08ab8", hp = 55f, speed = 70f,
        w = 14f, h = 20f, seeRange = 360f, contactKB = 220f,
        intelligence = 0.75f, bravery = 0.7f, canSpeak = true, lifeKind = LifeKind.SAPIENT, // proud — trades volleys, dodges yours
        dodge = DodgeSpec(0.30f, 0.18f, 1.8f),
        attacks = listOf(
            AttackSpec("twin_shot", cd = 1.6f, dmg = 9f, speed = 260f, life = 1.8f),
            AttackSpec("melee", cd = 1.8f, dmg = 12f, range = 13f, arc = 120f),
        ),
    ),
    "warden" to EnemyDef(
        name = "ウォーデン", tier = "normal", color = "#6a9a8a", hp = 110f, speed = 40f,
        w = 26f, h = 26f, seeRange = 280f, contactKB = 300f,
        bravery = 0.8f, protectiveness = 0.8f, intelligence = 0.55f, canSpeak = true, lifeKind = LifeKind.SAPIENT, // holds the door
        attacks = listOf(
            AttackSpec("shockwave", cd = 3.0f, dmg = 6f, range = 70f, power = 520f),
            AttackSpec("melee", cd = 1.2f, dmg = 10f, range = 14f, arc = 360f),
        ),
    ),
    // --- v2.48 惑星サーバー: the preservation machinery itself (appended — key order is contract).
    // Custodial processes given bodies. They do not hate the drifter; they evaluate him.
    "custodian" to EnemyDef(
        name = "保守ドローン", tier = "normal", color = "#7f8fa6", hp = 60f, speed = 50f,
        w = 17f, h = 17f, seeRange = 340f, contactKB = 220f,
        intelligence = 0.6f, bravery = 0.95f, // a procedure has no fear, only steps
        attacks = listOf(
            AttackSpec("shot", cd = 1.3f, dmg = 9f, speed = 230f),
            AttackSpec("mine", cd = 5.0f, dmg = 16f, life = 8f), // 封鎖ユニットを敷設して退く
        ),
    ),
    "indexer" to EnemyDef(
        name = "索引虫", tier = "normal", color = "#9ab8d0", hp = 35f, speed = 85f,
        w = 12f, h = 12f, seeRange = 300f, contactKB = 180f,
        intelligence = 0.4f, bravery = 0.7f, // it only wants to read you — quickly, twice
        attacks = listOf(
            AttackSpec("twin_shot", cd = 1.8f, dmg = 7f, speed = 250f, life = 1.6f),
            AttackSpec("melee", cd = 1.5f, dmg = 6f, range = 9f, arc = 120f),
        ),
    ),
    "quarantine" to EnemyDef(
        name = "検疫体", tier = "normal", color = "#6a7f97", hp = 130f, speed = 34f,
        w = 26f, h = 24f, seeRange = 280f, contactKB = 320f,
        intelligence = 0.5f, bravery = 0.9f, protectiveness = 0.7f, // isolate first, ask never
        attacks = listOf(
            AttackSpec("shockwave", cd = 3.4f, dmg = 7f, range = 75f, power = 500f), // 隔離波
            AttackSpec("spray", cd = 2.8f, dmg = 6f, count = 5, spread = 60f, speed = 200f, life = 1.4f),
        ),
    ),
)

private fun spaceBosses(): Map<String, EnemyDef> = mapOf(
    // --- Bosses (legacy enemies.js BUILTIN_BOSSES). tier midboss=5 attacks / boss=10. ---
    "brute" to EnemyDef(
        name = "ブルート(中ボス)", tier = "midboss", color = "#d08a3a", hp = 420f, speed = 58f,
        w = 34f, h = 34f, seeRange = 320f, contactKB = 320f, gravityResponse = 1.5f, // heavy — easy to fling into planets
        intelligence = 0.5f, bravery = 0.9f, // v2.42: rank carries smarts — cover, kiting, the works
        attacks = listOf(
            AttackSpec("melee", cd = 0.8f, dmg = 16f, range = 22f, arc = 360f),
            AttackSpec("charge_melee", cd = 3.2f, range = 60f, reach = 40f, windup = 0.8f, dmg = 30f, kb = 480f),
            AttackSpec("slam", cd = 4.0f, dmg = 18f, range = 80f, power = 360f),
            AttackSpec("burst", cd = 2.5f, dmg = 8f, count = 5, spread = 40f, speed = 240f),
            AttackSpec("summon", cd = 8.0f, minion = "zombie", count = 2),
            AttackSpec("shockwave", cd = 5.5f, dmg = 8f, range = 80f, power = 460f), // v2.42: peels off clingers
        ),
    ),
    "warlock" to EnemyDef(
        name = "ウォーロック(中ボス)", tier = "midboss", color = "#7a5ad0", hp = 360f, speed = 48f,
        w = 30f, h = 30f, seeRange = 360f, contactKB = 240f,
        intelligence = 0.85f, bravery = 0.6f, canHideAndRest = true, // v2.42: a scholar of the fight
        dodge = DodgeSpec(0.25f, 0.15f, 1.6f),
        attacks = listOf(
            AttackSpec("shot", cd = 1.0f, dmg = 12f, speed = 240f),
            AttackSpec("nova", cd = 5.0f, dmg = 8f, count = 14, speed = 180f),
            AttackSpec("blink", cd = 4.0f, maxTiles = 5, dur = 0.1f, minDist = 80f, standoff = 120f),
            AttackSpec("summon", cd = 7.0f, minion = "spitter", count = 2),
            AttackSpec("heal", cd = 9.0f, amount = 40f),
            AttackSpec("spray", cd = 3.5f, dmg = 6f, count = 5, spread = 60f, speed = 220f, life = 1.5f), // v2.42
        ),
    ),
    "overlord" to EnemyDef(
        name = "オーバーロード(ボス)", tier = "boss", color = "#d04a6a", hp = 1200f, speed = 52f,
        w = 46f, h = 46f, seeRange = 480f, contactKB = 360f, gravityResponse = 0.25f, // boss resists being flung
        intelligence = 0.9f, bravery = 1f, // v2.42: the throne earns its smarts
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
            AttackSpec("spiral", cd = 6.0f, dmg = 9f, count = 12, speed = 170f, life = 2.4f), // v2.42: the 11th move
        ),
    ),
    // v2.41: a siege-platform midboss built on the new attack kit (appended — key order is contract).
    "artillery" to EnemyDef(
        name = "アーティラリー(中ボス)", tier = "midboss", color = "#a08a50", hp = 380f, speed = 30f,
        w = 36f, h = 30f, seeRange = 460f, contactKB = 320f, gravityResponse = 1.2f,
        intelligence = 0.7f, bravery = 0.8f, // v2.42
        attacks = listOf(
            AttackSpec("barrage", cd = 3.0f, dmg = 8f, count = 7, spread = 70f, speed = 220f),
            AttackSpec("mine", cd = 4.0f, dmg = 20f, life = 8f),
            AttackSpec("shockwave", cd = 5.0f, dmg = 10f, range = 90f, power = 480f),
            AttackSpec("summon", cd = 9.0f, minion = "mortar_bug", count = 2),
            AttackSpec("guard", cd = 8.0f, duration = 2f, mul = 0.4f),
        ),
    ),
    // v2.48 惑星サーバー: the audit process itself, given a body (appended — key order is contract).
    "auditor" to EnemyDef(
        name = "監査体(中ボス)", tier = "midboss", color = "#8f9fc0", hp = 400f, speed = 44f,
        w = 32f, h = 32f, seeRange = 420f, contactKB = 280f,
        intelligence = 0.9f, bravery = 0.85f, // it has read every fight you ever had here
        dodge = DodgeSpec(0.28f, 0.16f, 1.7f),
        attacks = listOf(
            AttackSpec("shot", cd = 1.1f, dmg = 11f, speed = 250f),
            AttackSpec("spiral", cd = 5.0f, dmg = 8f, count = 12, speed = 170f, life = 2.2f),
            AttackSpec("summon", cd = 8.0f, minion = "custodian", count = 2),
            AttackSpec("guard", cd = 7.0f, duration = 2f, mul = 0.4f),
            AttackSpec("shockwave", cd = 5.5f, dmg = 9f, range = 85f, power = 470f),
        ),
    ),
)

private fun natureSociety(): Map<String, EnemyDef> = mapOf(
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
)

private fun magmaSociety(): Map<String, EnemyDef> = mapOf(
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
)

private fun iceSociety(): Map<String, EnemyDef> = mapOf(
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
)

private fun gasSociety(): Map<String, EnemyDef> = mapOf(
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
)

private fun deadSociety(): Map<String, EnemyDef> = mapOf(
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
)

private fun lonelyEncounters(): Map<String, EnemyDef> = mapOf(
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
)

private fun natureWildlife(): Map<String, EnemyDef> = mapOf(
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
)

private fun pioneerWildlife(): Map<String, EnemyDef> = mapOf(
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

private fun magmaWildlife(): Map<String, EnemyDef> = mapOf(
    // --- MAGMA: a molten food web — skittish lizards, a stone-eating herd, a serpent hunter, fragile hatchlings. ---
    "ash_lizard" to EnemyDef(
        name = "灰トカゲ", tier = "normal", color = "#c2734a", hp = 30f, speed = 100f, w = 18f, h = 16f,
        seeRange = 260f, contactKB = 120f, gravityResponse = 0.9f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.PREY, diet = Diet.ENERGY, herdAffinity = 0.4f, fear = 0.9f, biome = PlanetBiome.MAGMA,
    ),
    "basalt_ram" to EnemyDef(
        name = "玄武岩の角", tier = "normal", color = "#8a5a44", hp = 96f, speed = 76f, w = 26f, h = 24f,
        seeRange = 300f, contactKB = 240f, gravityResponse = 1.1f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.HERD, diet = Diet.MINERAL, herdAffinity = 0.85f, fear = 0.5f, biome = PlanetBiome.MAGMA,
    ),
    "lava_serpent" to EnemyDef(
        name = "溶岩の蛇", tier = "normal", color = "#e0612a", hp = 80f, speed = 98f, w = 24f, h = 20f,
        seeRange = 360f, contactKB = 240f, gravityResponse = 0.8f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.PREDATOR, diet = Diet.CARNIVORE, herdAffinity = 0.3f, fear = 0.2f, biome = PlanetBiome.MAGMA,
        attacks = listOf(
            AttackSpec("lunge", cd = 3.0f, range = 120f, power = 400f),
            AttackSpec("melee", cd = 0.9f, dmg = 14f, range = 18f, arc = 360f),
        ),
    ),
    "crater_hatchling" to EnemyDef(
        name = "火口の雛", tier = "normal", color = "#f0a050", hp = 18f, speed = 88f, w = 14f, h = 14f,
        seeRange = 220f, contactKB = 90f, gravityResponse = 0.8f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.HATCHLING, diet = Diet.OMNIVORE, herdAffinity = 0.7f, fear = 1.0f, biome = PlanetBiome.MAGMA,
    ),
)

private fun iceWildlife(): Map<String, EnemyDef> = mapOf(
    // --- ICE: prey, a shaggy herd, a stalking hunter, and a rare great worm at the top. ---
    "sleeping_calf" to EnemyDef(
        name = "眠り仔", tier = "normal", color = "#cfe2ee", hp = 40f, speed = 78f, w = 18f, h = 16f,
        seeRange = 240f, contactKB = 120f, gravityResponse = 1.0f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.PREY, diet = Diet.HERBIVORE, herdAffinity = 0.7f, fear = 0.8f, biome = PlanetBiome.ICE,
    ),
    "ice_muskox" to EnemyDef(
        name = "氷ジャコウウシ", tier = "normal", color = "#bcd6e6", hp = 110f, speed = 70f, w = 28f, h = 26f,
        seeRange = 300f, contactKB = 260f, gravityResponse = 1.1f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.HERD, diet = Diet.HERBIVORE, herdAffinity = 0.9f, fear = 0.5f, biome = PlanetBiome.ICE,
    ),
    "white_stalker" to EnemyDef(
        name = "白い追跡者", tier = "normal", color = "#dfeaf2", hp = 70f, speed = 100f, w = 22f, h = 20f,
        seeRange = 380f, contactKB = 240f, gravityResponse = 0.9f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.PREDATOR, diet = Diet.CARNIVORE, herdAffinity = 0.35f, fear = 0.2f, biome = PlanetBiome.ICE,
        attacks = listOf(
            AttackSpec("lunge", cd = 3.2f, range = 130f, power = 420f),
            AttackSpec("melee", cd = 0.9f, dmg = 13f, range = 16f, arc = 360f),
        ),
    ),
    "frost_worm" to EnemyDef(
        name = "霜の大蟲", tier = "normal", color = "#9fc4d8", hp = 320f, speed = 64f, w = 34f, h = 30f,
        seeRange = 400f, contactKB = 340f, gravityResponse = 0.7f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.APEX, diet = Diet.CARNIVORE, herdAffinity = 0.1f, fear = 0.05f, territoryRadius = 240f, biome = PlanetBiome.ICE,
        attacks = listOf(
            AttackSpec("charge_melee", cd = 3.4f, range = 90f, reach = 38f, windup = 0.7f, dmg = 22f, kb = 420f),
            AttackSpec("melee", cd = 0.8f, dmg = 18f, range = 26f, arc = 360f),
        ),
    ),
)

private fun gasWildlife(): Map<String, EnemyDef> = mapOf(
    // --- GAS: a floating, strange food web — drifting plankton, gliding rays, an electric eel hunter, frail whelps. ---
    "cloud_plankton" to EnemyDef(
        name = "雲プランクトン", tier = "normal", color = "#cfd8e8", hp = 14f, speed = 96f, w = 12f, h = 12f,
        seeRange = 220f, contactKB = 70f, gravityResponse = 0f, // gas dwellers float free of gravity
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.SWARM, diet = Diet.ENERGY, herdAffinity = 0.8f, fear = 0.95f, biome = PlanetBiome.GAS,
    ),
    "storm_ray" to EnemyDef(
        name = "嵐エイ", tier = "normal", color = "#a8b8d8", hp = 72f, speed = 84f, w = 28f, h = 22f,
        seeRange = 300f, contactKB = 180f, gravityResponse = 0f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.HERD, diet = Diet.ENERGY, herdAffinity = 0.85f, fear = 0.55f, biome = PlanetBiome.GAS,
    ),
    "thunder_eel" to EnemyDef(
        name = "雷ウナギ", tier = "normal", color = "#c8b0f0", hp = 78f, speed = 102f, w = 24f, h = 18f,
        seeRange = 360f, contactKB = 220f, gravityResponse = 0f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.PREDATOR, diet = Diet.CARNIVORE, herdAffinity = 0.3f, fear = 0.2f, biome = PlanetBiome.GAS,
        attacks = listOf(
            AttackSpec("lunge", cd = 3.0f, range = 120f, power = 380f),
            AttackSpec("melee", cd = 1.0f, dmg = 12f, range = 18f, arc = 360f),
        ),
    ),
    "gravity_whelp" to EnemyDef(
        name = "重力の仔", tier = "normal", color = "#b0c0e0", hp = 16f, speed = 90f, w = 13f, h = 13f,
        seeRange = 220f, contactKB = 80f, gravityResponse = 0f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.HATCHLING, diet = Diet.ENERGY, herdAffinity = 0.7f, fear = 1.0f, biome = PlanetBiome.GAS,
    ),
)

private fun deadWildlife(): Map<String, EnemyDef> = mapOf(
    // --- DEAD: a quiet scavenger/mimic ecosystem — carrion-eaters and ambushers, never crowded. ---
    "bone_rat" to EnemyDef(
        name = "骨ネズミ", tier = "normal", color = "#9a9488", hp = 22f, speed = 102f, w = 14f, h = 12f,
        seeRange = 240f, contactKB = 90f, gravityResponse = 0.9f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.SCAVENGER, diet = Diet.SCAVENGER, herdAffinity = 0.5f, fear = 0.9f, biome = PlanetBiome.DEAD,
    ),
    "ash_crow" to EnemyDef(
        name = "灰ガラス", tier = "normal", color = "#86807a", hp = 26f, speed = 110f, w = 16f, h = 14f,
        seeRange = 280f, contactKB = 90f, gravityResponse = 0.5f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.SCAVENGER, diet = Diet.SCAVENGER, herdAffinity = 0.75f, fear = 0.85f, biome = PlanetBiome.DEAD,
    ),
    "grave_mimic" to EnemyDef(
        name = "墓の擬態", tier = "normal", color = "#5d5a54", hp = 96f, speed = 70f, w = 24f, h = 22f,
        seeRange = 200f, contactKB = 260f, gravityResponse = 1.4f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.PREDATOR, diet = Diet.CARNIVORE, herdAffinity = 0.1f, fear = 0.1f, territoryRadius = 120f, biome = PlanetBiome.DEAD,
        attacks = listOf(
            AttackSpec("charge_melee", cd = 3.6f, range = 80f, reach = 32f, windup = 0.8f, dmg = 18f, kb = 360f),
        ),
    ),
    "ruin_parasite" to EnemyDef(
        name = "廃墟の寄生体", tier = "normal", color = "#7a6f86", hp = 38f, speed = 96f, w = 16f, h = 16f,
        seeRange = 300f, contactKB = 160f, gravityResponse = 0.8f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.PREDATOR, diet = Diet.CARNIVORE, herdAffinity = 0.4f, fear = 0.3f, biome = PlanetBiome.DEAD,
        attacks = listOf(
            AttackSpec("lunge", cd = 3.2f, range = 110f, power = 360f),
            AttackSpec("melee", cd = 1.0f, dmg = 10f, range = 16f, arc = 360f),
        ),
    ),
)

private fun lonelyWildlife(): Map<String, EnemyDef> = mapOf(
    // --- LONELY: sparse, mostly-noncombat encounters — a drifting moth, an old hound, a silent watcher, one last beast. ---
    "star_moth" to EnemyDef(
        name = "星蛾", tier = "normal", color = "#cdbfe6", hp = 18f, speed = 92f, w = 14f, h = 14f,
        seeRange = 220f, contactKB = 70f, gravityResponse = 0.4f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.PREY, diet = Diet.ENERGY, herdAffinity = 0.4f, fear = 0.95f, biome = PlanetBiome.LONELY,
    ),
    "old_hound" to EnemyDef(
        name = "老いた猟犬", tier = "normal", color = "#8a7f72", hp = 56f, speed = 74f, w = 22f, h = 18f,
        seeRange = 260f, contactKB = 160f, gravityResponse = 1.0f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.PREY, diet = Diet.OMNIVORE, herdAffinity = 0.2f, fear = 0.6f, biome = PlanetBiome.LONELY,
    ),
    "silent_watcher" to EnemyDef(
        name = "沈黙の見守り", tier = "normal", color = "#6f6a78", hp = 60f, speed = 60f, w = 20f, h = 22f,
        seeRange = 240f, contactKB = 140f, gravityResponse = 0.6f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.PREY, diet = Diet.NONE, herdAffinity = 0f, fear = 0.4f, biome = PlanetBiome.LONELY,
    ),
    "last_beast" to EnemyDef(
        name = "最後の獣", tier = "normal", color = "#544f5c", hp = 300f, speed = 66f, w = 34f, h = 30f,
        seeRange = 380f, contactKB = 320f, gravityResponse = 0.7f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.APEX, diet = Diet.CARNIVORE, herdAffinity = 0f, fear = 0.1f, territoryRadius = 220f, biome = PlanetBiome.LONELY,
        attacks = listOf(
            AttackSpec("charge_melee", cd = 3.8f, range = 90f, reach = 36f, windup = 0.8f, dmg = 20f, kb = 400f),
            AttackSpec("melee", cd = 0.9f, dmg = 16f, range = 24f, arc = 360f),
        ),
    ),
)


// ─── v2.82 expansion: 50 new species — sizes, smarts, speeds, tempers and voices ───────────

/** 15 new space normals + 6 new midbosses. Deep-desync material: the pool reaches them late. */
private fun expansionSpace(): Map<String, EnemyDef> = mapOf(
    // --- rank and file (biome = null, tier normal) ---
    "drift_leech" to EnemyDef(
        name = "漂流ヒル", tier = "normal", color = "#7d4a5e", hp = 34f, speed = 118f, w = 16f, h = 12f,
        seeRange = 300f, contactKB = 140f, gravityResponse = 0.3f,
        attacks = listOf(AttackSpec("melee", cd = 0.7f, dmg = 7f, range = 10f, arc = 360f)),
    ),
    "glass_mite" to EnemyDef(
        name = "硝子ダニ", tier = "normal", color = "#bcd8e8", hp = 16f, speed = 140f, w = 10f, h = 10f,
        seeRange = 260f, contactKB = 90f, gravityResponse = 0.2f,
        attacks = listOf(AttackSpec("lunge", cd = 2.2f, range = 70f, power = 300f)),
    ),
    "relay_husk" to EnemyDef(
        name = "中継の抜け殻", tier = "normal", color = "#5e6a78", hp = 160f, speed = 26f, w = 30f, h = 30f,
        seeRange = 360f, contactKB = 300f, gravityResponse = 1.2f,
        attacks = listOf(AttackSpec("shot", cd = 1.8f, dmg = 14f, speed = 190f, life = 2.0f)),
    ),
    "cinder_drone" to EnemyDef(
        name = "燼ドローン", tier = "normal", color = "#c96a3a", hp = 44f, speed = 84f, w = 18f, h = 14f,
        seeRange = 320f, contactKB = 160f, gravityResponse = 0f,
        attacks = listOf(AttackSpec("mine", cd = 3.4f, dmg = 16f, life = 6f)),
    ),
    "archive_moth" to EnemyDef(
        name = "書庫蛾", tier = "normal", color = "#c9bfae", hp = 40f, speed = 96f, w = 16f, h = 16f,
        seeRange = 300f, contactKB = 120f, gravityResponse = 0.2f,
        attacks = listOf(AttackSpec("spray", cd = 2.6f, dmg = 8f, speed = 200f, count = 3, spread = 0.5f)),
    ),
    "null_hound" to EnemyDef(
        name = "虚数の猟犬", tier = "normal", color = "#4d4a66", hp = 58f, speed = 112f, w = 22f, h = 16f,
        seeRange = 380f, contactKB = 220f, gravityResponse = 0.6f,
        intelligence = 0.4f, canSpeak = true, speechStyle = "savage", lifeKind = LifeKind.SAPIENT,
        attacks = listOf(
            AttackSpec("lunge", cd = 2.8f, range = 110f, power = 420f),
            AttackSpec("melee", cd = 0.8f, dmg = 12f, range = 14f, arc = 180f),
        ),
    ),
    "patch_crab" to EnemyDef(
        name = "修繕蟹", tier = "normal", color = "#7a9a8a", hp = 90f, speed = 46f, w = 24f, h = 18f,
        seeRange = 280f, contactKB = 240f, gravityResponse = 1.0f,
        intelligence = 0.7f, bravery = 0.6f, canSpeak = true, speechStyle = "mechanical", lifeKind = LifeKind.SAPIENT,
        attacks = listOf(
            AttackSpec("guard", cd = 6f, duration = 1.6f, mul = 0.4f),
            AttackSpec("melee", cd = 1.1f, dmg = 13f, range = 16f, arc = 120f),
        ),
    ),
    "echo_wisp" to EnemyDef(
        name = "残響ウィスプ", tier = "normal", color = "#9fd2e8", hp = 36f, speed = 90f, w = 14f, h = 14f,
        seeRange = 340f, contactKB = 100f, gravityResponse = 0f,
        dodge = DodgeSpec(0.22f, 0.12f, 1.8f),
        attacks = listOf(
            AttackSpec("blink", cd = 2.6f, maxTiles = 4, dur = 0.1f, minDist = 60f, standoff = 30f),
            AttackSpec("twin_shot", cd = 1.6f, dmg = 9f, speed = 230f, life = 1.4f),
        ),
    ),
    "ballast_golem" to EnemyDef(
        name = "錘のゴーレム", tier = "normal", color = "#5a5450", hp = 220f, speed = 20f, w = 34f, h = 34f,
        seeRange = 260f, contactKB = 380f, gravityResponse = 1.6f,
        attacks = listOf(AttackSpec("slam", cd = 3.2f, dmg = 22f, range = 60f, windup = 0.9f, kb = 420f)),
    ),
    "sweeper_scarab" to EnemyDef(
        name = "掃討スカラベ", tier = "normal", color = "#8a7a3a", hp = 70f, speed = 92f, w = 20f, h = 16f,
        seeRange = 320f, contactKB = 200f, gravityResponse = 0.8f,
        attacks = listOf(AttackSpec("charge", cd = 3.6f, range = 200f, power = 520f, windup = 0.7f, dmg = 15f)),
    ),
    "cipher_ray" to EnemyDef(
        name = "暗号エイ", tier = "normal", color = "#6a8ad0", hp = 52f, speed = 76f, w = 26f, h = 12f,
        seeRange = 400f, contactKB = 140f, gravityResponse = 0f,
        attacks = listOf(AttackSpec("homing", cd = 3.0f, dmg = 11f, speed = 150f, turn = 2.4f, life = 3f)),
    ),
    "seal_moth" to EnemyDef(
        name = "封蝋蛾", tier = "normal", color = "#b06a8a", hp = 48f, speed = 68f, w = 18f, h = 18f,
        seeRange = 280f, contactKB = 150f, gravityResponse = 0.3f,
        attacks = listOf(AttackSpec("nova", cd = 3.8f, dmg = 12f, count = 8, speed = 170f, life = 1.2f)),
    ),
    "tally_keeper" to EnemyDef(
        name = "計数員", tier = "normal", color = "#8a94a8", hp = 64f, speed = 54f, w = 20f, h = 22f,
        seeRange = 360f, contactKB = 180f, gravityResponse = 0.9f,
        intelligence = 0.9f, bravery = 0.3f, canBeg = true, mercyThreshold = 0.35f,
        canSpeak = true, speechStyle = "mechanical", lifeKind = LifeKind.SAPIENT,
        attacks = listOf(
            AttackSpec("shot", cd = 1.4f, dmg = 11f, speed = 240f, life = 1.6f),
            AttackSpec("guard", cd = 7f, duration = 1.2f, mul = 0.5f),
        ),
    ),
    "grief_shell" to EnemyDef(
        name = "悼みの殻", tier = "normal", color = "#6f7a8e", hp = 110f, speed = 32f, w = 26f, h = 26f,
        seeRange = 300f, contactKB = 260f, gravityResponse = 1.1f,
        intelligence = 0.6f, bravery = 0.8f, canSpeak = true, speechStyle = "archaic", lifeKind = LifeKind.SAPIENT,
        attacks = listOf(AttackSpec("burst", cd = 2.8f, dmg = 10f, count = 4, speed = 200f, spread = 0.4f)),
    ),
    "vector_imp" to EnemyDef(
        name = "矢印の小鬼", tier = "normal", color = "#d0a04a", hp = 30f, speed = 128f, w = 14f, h = 14f,
        seeRange = 300f, contactKB = 110f, gravityResponse = 0.4f,
        attacks = listOf(AttackSpec("spiral", cd = 3.4f, dmg = 8f, count = 6, speed = 160f, life = 1.6f)),
    ),
    // --- new midbosses (rotation tail) ---
    "archivist" to EnemyDef(
        name = "書庫長", tier = "midboss", color = "#a89a72", hp = 620f, speed = 40f, w = 34f, h = 38f,
        seeRange = 460f, contactKB = 300f, gravityResponse = 0.8f,
        intelligence = 0.9f, canSpeak = true, speechStyle = "mechanical", lifeKind = LifeKind.SAPIENT,
        attacks = listOf(
            AttackSpec("summon", cd = 7f, count = 3, minion = "archive_moth"),
            AttackSpec("barrage", cd = 4.2f, dmg = 10f, count = 7, speed = 210f, spread = 0.9f),
            // v2.94 固有技: 頁の壁 — a written line advancing abreast; dodge past its edges.
            AttackSpec("page_wall", cd = 6.5f, dmg = 12f, count = 7, spread = 30f, speed = 130f, life = 3.2f),
        ),
    ),
    "tide_warden" to EnemyDef(
        name = "潮の番人", tier = "midboss", color = "#4a8ab0", hp = 700f, speed = 36f, w = 36f, h = 36f,
        seeRange = 420f, contactKB = 340f, gravityResponse = 0.9f,
        attacks = listOf(
            AttackSpec("shockwave", cd = 5f, dmg = 16f, range = 150f, windup = 1.0f, kb = 460f),
            AttackSpec("heal", cd = 9f, amount = 60f),
        ),
    ),
    "hollow_knight" to EnemyDef(
        name = "空洞の騎士", tier = "midboss", color = "#7a7f96", hp = 640f, speed = 58f, w = 30f, h = 36f,
        seeRange = 440f, contactKB = 320f, gravityResponse = 1.0f,
        intelligence = 0.7f, canSpeak = true, speechStyle = "archaic", lifeKind = LifeKind.SAPIENT,
        attacks = listOf(
            AttackSpec("charge_melee", cd = 3.0f, range = 120f, reach = 44f, windup = 0.7f, dmg = 24f, kb = 480f),
            AttackSpec("guard", cd = 8f, duration = 2f, mul = 0.35f),
        ),
    ),
    "chorus_node" to EnemyDef(
        name = "合唱ノード", tier = "midboss", color = "#b08ad0", hp = 560f, speed = 44f, w = 32f, h = 32f,
        seeRange = 480f, contactKB = 260f, gravityResponse = 0f,
        attacks = listOf(
            AttackSpec("spiral", cd = 3.6f, dmg = 9f, count = 10, speed = 170f, life = 2f),
            AttackSpec("summon", cd = 8f, count = 2, minion = "echo_wisp"),
            // v2.94 固有技: 退路封じ — the echo lands ahead of the runner and sings back.
            AttackSpec("cutoff_volley", cd = 5.5f, dmg = 11f, count = 3, speed = 200f, life = 2.4f),
        ),
    ),
    "rust_titan" to EnemyDef(
        name = "錆の巨人", tier = "midboss", color = "#8a5a3a", hp = 900f, speed = 26f, w = 42f, h = 42f,
        seeRange = 380f, contactKB = 420f, gravityResponse = 1.4f,
        attacks = listOf(
            AttackSpec("slam", cd = 3.4f, dmg = 26f, range = 80f, windup = 1.0f, kb = 520f),
            AttackSpec("enrage", cd = 12f, duration = 4f, mul = 1.5f),
            // v2.94 固有技: 輪の裂け目 — a full ring with one silence; read it and dash through.
            AttackSpec("ring_gap", cd = 7f, dmg = 13f, count = 18, spread = 70f, speed = 150f, life = 2.6f),
        ),
    ),
    "lantern_bearer" to EnemyDef(
        name = "提灯持ち", tier = "midboss", color = "#d0b04a", hp = 580f, speed = 50f, w = 28f, h = 34f,
        seeRange = 460f, contactKB = 280f, gravityResponse = 0.6f,
        intelligence = 0.8f, canSpeak = true, speechStyle = "polite", lifeKind = LifeKind.SAPIENT,
        attacks = listOf(
            AttackSpec("homing", cd = 3.2f, dmg = 13f, speed = 140f, turn = 2.8f, life = 3.5f),
            AttackSpec("mine", cd = 5f, dmg = 18f, life = 8f),
        ),
    ),
)

/** v2.83: a drifter like you — someone else's last keeper, and no friend of anyone's. */
private fun rogueDrifter(): Map<String, EnemyDef> = mapOf(
    "rogue_drifter" to EnemyDef(
        // tier "rogue": kept OUT of the surge pool / drifter rolls / wreck pickets — it only
        // ever enters the world through its own two-per-system spawn, under the ROGUE banner.
        name = "ならず者の漂流者", tier = "rogue", color = "#d8e2ec", hp = 280f, speed = 98f, w = 24f, h = 24f,
        seeRange = 460f, contactKB = 240f, gravityResponse = 0.5f,
        intelligence = 0.9f, bravery = 0.9f, canSpeak = true, speechStyle = "savage", lifeKind = LifeKind.SAPIENT,
        dodge = DodgeSpec(0.22f, 0.13f, 1.8f),
        attacks = listOf(
            AttackSpec("twin_shot", cd = 0.9f, dmg = 12f, speed = 290f, life = 1.8f),
            AttackSpec("blink", cd = 4f, maxTiles = 5, dur = 0.1f, minDist = 80f, standoff = 40f),
            AttackSpec("charge_melee", cd = 3.4f, range = 90f, reach = 34f, windup = 0.6f, dmg = 20f, kb = 420f),
        ),
    ),
)

/** 18 new society members — three per biome, each with its own temper and register. */
private fun expansionSociety(): Map<String, EnemyDef> = mapOf(
    // NATURE
    "thorn_sentinel" to EnemyDef(
        name = "茨の哨兵", tier = "normal", color = "#4a7a3a", hp = 96f, speed = 66f, w = 24f, h = 26f,
        seeRange = 340f, contactKB = 260f, biome = PlanetBiome.NATURE,
        intelligence = 0.5f, bravery = 0.9f, protectiveness = 0.8f, canSpeak = true, speechStyle = "savage", lifeKind = LifeKind.SAPIENT,
        attacks = listOf(AttackSpec("melee", cd = 1.0f, dmg = 15f, range = 20f, arc = 150f)),
    ),
    "seed_keeper" to EnemyDef(
        name = "種の番人", tier = "normal", color = "#7aa05a", hp = 70f, speed = 52f, w = 20f, h = 22f,
        seeRange = 320f, contactKB = 180f, biome = PlanetBiome.NATURE,
        intelligence = 0.8f, bravery = 0.4f, canBeg = true, mercyThreshold = 0.3f, canSpeak = true, speechStyle = "polite", lifeKind = LifeKind.SAPIENT,
        attacks = listOf(AttackSpec("heal", cd = 8f, amount = 40f), AttackSpec("shot", cd = 1.8f, dmg = 9f, speed = 200f, life = 1.5f)),
    ),
    "canopy_archer" to EnemyDef(
        name = "梢の射手", tier = "normal", color = "#5a8a4a", hp = 60f, speed = 84f, w = 18f, h = 22f,
        seeRange = 420f, contactKB = 160f, biome = PlanetBiome.NATURE,
        intelligence = 0.6f, bravery = 0.5f, canHideAndRest = true, canSpeak = true, lifeKind = LifeKind.SAPIENT,
        dodge = DodgeSpec(0.16f, 0.14f, 2.2f),
        attacks = listOf(AttackSpec("shot", cd = 1.3f, dmg = 13f, speed = 260f, life = 1.8f)),
    ),
    // MAGMA
    "forge_priest" to EnemyDef(
        name = "炉の司祭", tier = "normal", color = "#c05a2a", hp = 84f, speed = 48f, w = 22f, h = 26f,
        seeRange = 340f, contactKB = 220f, biome = PlanetBiome.MAGMA,
        intelligence = 0.8f, bravery = 0.7f, canSpeak = true, speechStyle = "archaic", lifeKind = LifeKind.SAPIENT,
        attacks = listOf(AttackSpec("nova", cd = 4f, dmg = 12f, count = 6, speed = 160f, life = 1.4f), AttackSpec("heal", cd = 10f, amount = 50f)),
    ),
    "slag_brute" to EnemyDef(
        name = "鉱滓の荒くれ", tier = "normal", color = "#8a4a2a", hp = 150f, speed = 58f, w = 28f, h = 28f,
        seeRange = 300f, contactKB = 320f, biome = PlanetBiome.MAGMA,
        intelligence = 0.2f, bravery = 1f, canSpeak = true, speechStyle = "savage", lifeKind = LifeKind.SAPIENT,
        attacks = listOf(AttackSpec("charge_melee", cd = 3.2f, range = 100f, reach = 38f, windup = 0.8f, dmg = 20f, kb = 440f)),
    ),
    "ember_dancer" to EnemyDef(
        name = "熾の踊り手", tier = "normal", color = "#e08a4a", hp = 56f, speed = 104f, w = 16f, h = 20f,
        seeRange = 320f, contactKB = 150f, biome = PlanetBiome.MAGMA,
        intelligence = 0.5f, bravery = 0.6f, canSpeak = true, lifeKind = LifeKind.SAPIENT,
        dodge = DodgeSpec(0.20f, 0.12f, 1.8f),
        attacks = listOf(AttackSpec("spray", cd = 2.4f, dmg = 9f, speed = 190f, count = 3, spread = 0.6f)),
    ),
    // ICE
    "glacier_monk" to EnemyDef(
        name = "氷河の修道士", tier = "normal", color = "#9ab8d0", hp = 78f, speed = 44f, w = 22f, h = 26f,
        seeRange = 320f, contactKB = 200f, biome = PlanetBiome.ICE,
        intelligence = 0.9f, bravery = 0.5f, canBeg = true, mercyThreshold = 0.4f, canSpeak = true, speechStyle = "polite", lifeKind = LifeKind.SAPIENT,
        attacks = listOf(AttackSpec("guard", cd = 7f, duration = 1.8f, mul = 0.4f), AttackSpec("shot", cd = 1.6f, dmg = 11f, speed = 220f, life = 1.6f)),
    ),
    "shard_lancer" to EnemyDef(
        name = "氷片の槍騎兵", tier = "normal", color = "#7aa0c8", hp = 88f, speed = 78f, w = 22f, h = 24f,
        seeRange = 360f, contactKB = 260f, biome = PlanetBiome.ICE,
        intelligence = 0.5f, bravery = 0.8f, canSpeak = true, lifeKind = LifeKind.SAPIENT,
        attacks = listOf(AttackSpec("charge_melee", cd = 2.8f, range = 130f, reach = 40f, windup = 0.6f, dmg = 18f, kb = 400f)),
    ),
    "aurora_seer" to EnemyDef(
        name = "極光の巫", tier = "normal", color = "#a0d0c0", hp = 62f, speed = 50f, w = 18f, h = 24f,
        seeRange = 400f, contactKB = 160f, biome = PlanetBiome.ICE,
        intelligence = 0.9f, bravery = 0.3f, canHideAndRest = true, canSpeak = true, speechStyle = "archaic", lifeKind = LifeKind.SAPIENT,
        attacks = listOf(AttackSpec("homing", cd = 3.4f, dmg = 12f, speed = 130f, turn = 2.2f, life = 3f)),
    ),
    // GAS
    "wind_cantor" to EnemyDef(
        name = "風の聖歌員", tier = "normal", color = "#b0c8a0", hp = 58f, speed = 72f, w = 18f, h = 22f,
        seeRange = 360f, contactKB = 140f, gravityResponse = 0f, biome = PlanetBiome.GAS,
        intelligence = 0.7f, bravery = 0.4f, canSpeak = true, speechStyle = "polite", lifeKind = LifeKind.SAPIENT,
        attacks = listOf(AttackSpec("spiral", cd = 3.6f, dmg = 9f, count = 6, speed = 150f, life = 1.8f)),
    ),
    "storm_herald" to EnemyDef(
        name = "嵐の触れ役", tier = "normal", color = "#8a9ad0", hp = 74f, speed = 88f, w = 20f, h = 20f,
        seeRange = 400f, contactKB = 180f, gravityResponse = 0f, biome = PlanetBiome.GAS,
        intelligence = 0.6f, bravery = 0.7f, canSpeak = true, speechStyle = "savage", lifeKind = LifeKind.SAPIENT,
        attacks = listOf(AttackSpec("twin_shot", cd = 1.8f, dmg = 10f, speed = 240f, life = 1.6f)),
    ),
    "pressure_warden" to EnemyDef(
        name = "気圧の番人", tier = "normal", color = "#6a7a96", hp = 120f, speed = 40f, w = 26f, h = 26f,
        seeRange = 320f, contactKB = 300f, gravityResponse = 0f, biome = PlanetBiome.GAS,
        intelligence = 0.8f, bravery = 0.9f, protectiveness = 0.7f, canSpeak = true, speechStyle = "mechanical", lifeKind = LifeKind.SAPIENT,
        attacks = listOf(AttackSpec("shockwave", cd = 4.6f, dmg = 14f, range = 120f, windup = 0.9f, kb = 420f)),
    ),
    // DEAD
    "bone_scribe" to EnemyDef(
        name = "骨の書記", tier = "normal", color = "#9a948a", hp = 66f, speed = 46f, w = 20f, h = 24f,
        seeRange = 340f, contactKB = 180f, biome = PlanetBiome.DEAD,
        intelligence = 0.9f, bravery = 0.4f, canBeg = true, mercyThreshold = 0.35f, canSpeak = true, speechStyle = "mechanical", lifeKind = LifeKind.SAPIENT,
        attacks = listOf(AttackSpec("shot", cd = 1.5f, dmg = 12f, speed = 210f, life = 1.7f)),
    ),
    "grave_keeper" to EnemyDef(
        name = "墓守", tier = "normal", color = "#6a655c", hp = 130f, speed = 42f, w = 26f, h = 28f,
        seeRange = 300f, contactKB = 300f, biome = PlanetBiome.DEAD,
        intelligence = 0.6f, bravery = 0.9f, protectiveness = 0.9f, canSpeak = true, speechStyle = "archaic", lifeKind = LifeKind.SAPIENT,
        attacks = listOf(AttackSpec("slam", cd = 3.2f, dmg = 18f, range = 60f, windup = 0.9f, kb = 400f)),
    ),
    "ash_pilgrim" to EnemyDef(
        name = "灰の巡礼", tier = "normal", color = "#8a8478", hp = 54f, speed = 60f, w = 18f, h = 22f,
        seeRange = 320f, contactKB = 150f, biome = PlanetBiome.DEAD,
        intelligence = 0.7f, bravery = 0.2f, canBeg = true, mercyThreshold = 0.5f, canHideAndRest = true,
        canSpeak = true, speechStyle = "polite", lifeKind = LifeKind.SAPIENT,
        attacks = listOf(AttackSpec("melee", cd = 1.4f, dmg = 9f, range = 14f, arc = 120f)),
    ),
    // LONELY
    "waymark_tender" to EnemyDef(
        name = "道標の手入れ人", tier = "normal", color = "#a09a8a", hp = 60f, speed = 50f, w = 20f, h = 22f,
        seeRange = 300f, contactKB = 160f, biome = PlanetBiome.LONELY,
        intelligence = 0.8f, bravery = 0.3f, canBeg = true, mercyThreshold = 0.4f, canSpeak = true, speechStyle = "polite", lifeKind = LifeKind.SAPIENT,
        attacks = listOf(AttackSpec("melee", cd = 1.5f, dmg = 8f, range = 14f, arc = 120f)),
    ),
    "echo_hermit" to EnemyDef(
        name = "木霊の隠者", tier = "normal", color = "#7a8a96", hp = 72f, speed = 44f, w = 20f, h = 24f,
        seeRange = 360f, contactKB = 170f, biome = PlanetBiome.LONELY,
        intelligence = 0.9f, bravery = 0.4f, canHideAndRest = true, canSpeak = true, speechStyle = "archaic", lifeKind = LifeKind.SAPIENT,
        attacks = listOf(AttackSpec("burst", cd = 3f, dmg = 9f, count = 3, speed = 190f, spread = 0.3f)),
    ),
    "stray_knight" to EnemyDef(
        name = "はぐれ騎士", tier = "normal", color = "#96865c", hp = 140f, speed = 62f, w = 26f, h = 28f,
        seeRange = 340f, contactKB = 300f, biome = PlanetBiome.LONELY,
        intelligence = 0.5f, bravery = 1f, canSpeak = true, speechStyle = "savage", lifeKind = LifeKind.SAPIENT,
        attacks = listOf(
            AttackSpec("charge_melee", cd = 3f, range = 110f, reach = 40f, windup = 0.7f, dmg = 22f, kb = 460f),
            AttackSpec("guard", cd = 8f, duration = 1.5f, mul = 0.4f),
        ),
    ),
)

/** 11 new wild species — new prey, herds and hunters woven into the existing food webs. */
private fun expansionWildlife(): Map<String, EnemyDef> = mapOf(
    "river_otter" to EnemyDef(
        name = "川獺", tier = "normal", color = "#6a5a48", hp = 30f, speed = 100f, w = 16f, h = 12f,
        seeRange = 240f, contactKB = 90f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.PREY, diet = Diet.OMNIVORE, herdAffinity = 0.3f, fear = 0.8f, biome = PlanetBiome.NATURE,
    ),
    "thorn_tortoise" to EnemyDef(
        name = "茨亀", tier = "normal", color = "#5c6a4a", hp = 120f, speed = 22f, w = 26f, h = 20f,
        seeRange = 180f, contactKB = 260f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.HERD, diet = Diet.HERBIVORE, herdAffinity = 0.5f, fear = 0.2f, biome = PlanetBiome.NATURE,
    ),
    "bramble_lynx" to EnemyDef(
        name = "荊山猫", tier = "normal", color = "#7a6a4a", hp = 66f, speed = 108f, w = 22f, h = 16f,
        seeRange = 380f, contactKB = 200f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.PREDATOR, diet = Diet.CARNIVORE, herdAffinity = 0.2f, fear = 0.3f, biome = PlanetBiome.NATURE,
    ),
    "ashwing" to EnemyDef(
        name = "灰翼", tier = "normal", color = "#b08a6a", hp = 22f, speed = 116f, w = 14f, h = 12f,
        seeRange = 260f, contactKB = 80f, gravityResponse = 0.3f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.PREY, diet = Diet.ENERGY, herdAffinity = 0.6f, fear = 0.9f, biome = PlanetBiome.MAGMA,
    ),
    "snow_owl" to EnemyDef(
        name = "雪梟", tier = "normal", color = "#e8eef4", hp = 26f, speed = 110f, w = 16f, h = 14f,
        seeRange = 320f, contactKB = 90f, gravityResponse = 0.4f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.PREY, diet = Diet.CARNIVORE, herdAffinity = 0.1f, fear = 0.85f, biome = PlanetBiome.ICE,
    ),
    "rime_elk" to EnemyDef(
        name = "霧氷の鹿", tier = "normal", color = "#b8c8d4", hp = 90f, speed = 64f, w = 26f, h = 24f,
        seeRange = 280f, contactKB = 220f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.HERD, diet = Diet.HERBIVORE, herdAffinity = 0.7f, fear = 0.6f, biome = PlanetBiome.ICE,
    ),
    "rime_wolf" to EnemyDef(
        name = "凍て狼", tier = "normal", color = "#9ab0c4", hp = 68f, speed = 102f, w = 22f, h = 18f,
        seeRange = 380f, contactKB = 220f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.PREDATOR, diet = Diet.CARNIVORE, herdAffinity = 0.5f, fear = 0.25f, biome = PlanetBiome.ICE,
    ),
    "sky_grazer" to EnemyDef(
        name = "空の放牧獣", tier = "normal", color = "#c0b890", hp = 140f, speed = 36f, w = 32f, h = 24f,
        seeRange = 220f, contactKB = 260f, gravityResponse = 0f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.HERD, diet = Diet.ENERGY, herdAffinity = 0.8f, fear = 0.5f, biome = PlanetBiome.GAS,
    ),
    "crypt_beetle" to EnemyDef(
        name = "納骨堂の甲虫", tier = "normal", color = "#5c584e", hp = 34f, speed = 78f, w = 16f, h = 12f,
        seeRange = 200f, contactKB = 120f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.PREY, diet = Diet.OMNIVORE, herdAffinity = 0.4f, fear = 0.7f, biome = PlanetBiome.DEAD,
    ),
    "tomb_stalker" to EnemyDef(
        name = "墓所の徘徊者", tier = "normal", color = "#4e4a56", hp = 84f, speed = 88f, w = 24f, h = 18f,
        seeRange = 340f, contactKB = 240f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.PREDATOR, diet = Diet.CARNIVORE, herdAffinity = 0.1f, fear = 0.2f, territoryRadius = 140f, biome = PlanetBiome.DEAD,
    ),
    "dust_skipper" to EnemyDef(
        name = "塵跳ね", tier = "normal", color = "#b0a890", hp = 20f, speed = 120f, w = 12f, h = 10f,
        seeRange = 220f, contactKB = 70f,
        lifeKind = LifeKind.WILDLIFE, wildRole = WildRole.PREY, diet = Diet.OMNIVORE, herdAffinity = 0.3f, fear = 0.95f, biome = PlanetBiome.LONELY,
    ),
)
