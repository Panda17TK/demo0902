package io.github.panda17tk.arpg.item

/**
 * Every item that exists in this world (v2.33): thrusters, armours, the classic guns as
 * RANGED_WEAPON items (their 種類 = [ItemDef.weaponType]), melee arms, and accessories.
 * Pure data — the single source of item names/stats.
 */
object ItemCatalog {
    val ALL: List<ItemDef> = listOf(
        // --- 推進器 (thrusters) ---
        ItemDef("thruster_std", "標準スラスター", ItemKind.THRUSTER, desc = "癖のない推進器"),
        ItemDef("thruster_light", "軽量スラスター", ItemKind.THRUSTER, desc = "鋭い立ち上がり　巡航は控えめ", accelMul = 1.3f, cruiseMul = 0.9f),
        ItemDef("thruster_heavy", "大推力スラスター", ItemKind.THRUSTER, desc = "重いが速い", accelMul = 0.8f, cruiseMul = 1.2f),
        ItemDef("thruster_oc", "OCスラスター", ItemKind.THRUSTER, desc = "フルスロットル可能　全開時 推力3倍/巡航2倍/スタミナ消費2倍", thrusterClass = ThrusterClass.OC),
        // --- 防具 (armor) ---
        ItemDef("armor_cloth", "パイロットスーツ", ItemKind.ARMOR, desc = "身軽な標準装備", damageMul = 0.95f),
        ItemDef("armor_light", "軽装甲", ItemKind.ARMOR, desc = "被ダメージ -15%", damageMul = 0.85f),
        ItemDef("armor_combat", "戦闘装甲", ItemKind.ARMOR, desc = "被ダメージ -30%　少し重い", damageMul = 0.7f, moveMul = 0.95f),
        ItemDef("armor_heavy", "重装甲", ItemKind.ARMOR, desc = "被ダメージ -45%　重い", damageMul = 0.55f, moveMul = 0.85f),
        // --- 遠距離武器 (the classic guns as items; 種類 = weaponType) ---
        ItemDef("gun_pistol", "ピストル", ItemKind.RANGED_WEAPON, desc = "種類:ピストル　無限予備弾", weaponType = "pistol"),
        ItemDef("gun_shotgun", "ショットガン", ItemKind.RANGED_WEAPON, desc = "種類:ショットガン", weaponType = "shotgun"),
        ItemDef("gun_mg", "マシンガン", ItemKind.RANGED_WEAPON, desc = "種類:マシンガン", weaponType = "mg"),
        ItemDef("gun_beam", "ビーム", ItemKind.RANGED_WEAPON, desc = "種類:ビーム　貫通+壁爆破", weaponType = "beam"),
        ItemDef("gun_grenade", "グレネード", ItemKind.RANGED_WEAPON, desc = "種類:グレネード", weaponType = "grenade"),
        // --- 近距離武器 (melee) ---
        ItemDef("melee_knife", "コンバットナイフ", ItemKind.MELEE_WEAPON, desc = "素早い標準ナイフ"),
        ItemDef("melee_blade", "プラズマブレード", ItemKind.MELEE_WEAPON, desc = "近接ダメージ +30%", meleeDmgMul = 1.3f),
        ItemDef("melee_lance", "重力ランス", ItemKind.MELEE_WEAPON, desc = "リーチ +40%", meleeReachMul = 1.4f, meleeDmgMul = 1.1f),
        // --- 装飾品 (accessories ×3 slots) ---
        ItemDef("acc_boots", "疾駆のブーツ", ItemKind.ACCESSORY, desc = "移動速度 +10%", moveMul = 1.1f),
        ItemDef("acc_charm", "再生の護符", ItemKind.ACCESSORY, desc = "スタミナ回復 +30%", staRegenMul = 1.3f),
        ItemDef("acc_scope", "精密スコープ", ItemKind.ACCESSORY, desc = "銃ダメージ +10%", gunMul = 1.1f),
        ItemDef("acc_plating", "追加プレート", ItemKind.ACCESSORY, desc = "被ダメージ -10%", damageMul = 0.9f),
        ItemDef("acc_feather", "星渡りの羽", ItemKind.ACCESSORY, desc = "移動 +5% 回復 +10%", moveMul = 1.05f, staRegenMul = 1.1f),
        ItemDef("acc_core", "獣王の牙", ItemKind.ACCESSORY, desc = "近接 +15% 銃 +5%", meleeDmgMul = 1.15f, gunMul = 1.05f),
    )

    private val byId = ALL.associateBy { it.id }
    fun byId(id: String): ItemDef? = byId[id]

    /** The RANGED_WEAPON item that behaves as the given WeaponDef id (pistol/shotgun/…). */
    fun byWeaponType(weaponType: String): ItemDef? = ALL.firstOrNull { it.weaponType == weaponType }

    /** What a fresh run wears: pistol + knife on a standard thruster in a pilot suit. */
    fun starterLoadout(): Loadout = Loadout(
        thruster = byId("thruster_std"),
        armor = byId("armor_cloth"),
        ranged = byId("gun_pistol"),
        melee = byId("melee_knife"),
    )

    /** What a fresh run carries: the rest of the classic guns + an OC thruster to try full throttle. */
    fun starterBackpack(): MutableList<ItemDef> = mutableListOf(
        byId("gun_shotgun")!!, byId("gun_mg")!!, byId("gun_beam")!!, byId("gun_grenade")!!,
        byId("thruster_oc")!!,
    )

    /** A deterministic drop pick (kill loot rolls an index; equipment enters the world as spoils). */
    fun dropFor(roll: Int): ItemDef = ALL[((roll % ALL.size) + ALL.size) % ALL.size]
}
