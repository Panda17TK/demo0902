package io.github.panda17tk.arpg.item

/** The seven equipment slots (v2.33): thruster / armor / ranged / melee / three accessories. */
enum class EquipSlot { THRUSTER, ARMOR, RANGED, MELEE, ACC1, ACC2, ACC3 }

/** What an item IS — which slot family it can occupy. CONSUMABLE is used from the ITEMS tab;
 *  LORE (読み物 — v2.34) is a readable that deepens the world and is never equipped or spent. */
enum class ItemKind { THRUSTER, ARMOR, RANGED_WEAPON, MELEE_WEAPON, ACCESSORY, CONSUMABLE, LORE }

/** Thruster classes: OC (overclocked) thrusters unlock the full-throttle mode. */
enum class ThrusterClass { STANDARD, OC }

/** What using a CONSUMABLE does (v2.34). The magnitude rides in [ItemDef.power] — for the timed
 *  resistances (v2.35: HEAT_PROOF / COLD_PROOF / MAGNET / REGEN) it is the duration in seconds. */
enum class ConsumeKind { NONE, HEAL, STAMINA, STAMINA_INF, DASH_UP, SMOKE, BLOCKS, AMMO_ALL, HEAT_PROOF, COLD_PROOF, MAGNET, REGEN }

/**
 * On/off special effects a piece of equipment can carry (v2.35). Wearing ANY equipped item with
 * the trait grants it outright:
 *  - HEAT_PROOF: magma damage is cut completely (溶岩床・マグマ惑星の熱を無効)
 *  - COLD_PROOF: snow no longer slows and ice no longer slides (雪の減速・氷の滑りを無効)
 *  - MAGNET: pickups are collected from much further away (ドロップ品を引き寄せる)
 *  - CRASH_PROOF: high-speed wall/planet impacts cost no HP (激突ダメージ無効, v2.38)
 */
enum class ItemTrait { HEAT_PROOF, COLD_PROOF, MAGNET, CRASH_PROOF }

/**
 * One item in the world (v2.33 装備/インベントリ). Pure data — behaviour stays in the systems that
 * read the aggregated multipliers off [Loadout]. The old pistol/shotgun/… weapons live on as
 * RANGED_WEAPON items whose [weaponType] names the WeaponDef they behave as (種類 attribute).
 */
data class ItemDef(
    val id: String,
    val name: String,
    val kind: ItemKind,
    val desc: String = "",
    // RANGED_WEAPON: which WeaponDef id this gun behaves as (pistol / shotgun / mg / beam / grenade).
    val weaponType: String? = null,
    // RANGED_WEAPON grades (v2.37): per-gun handling tweaks, applied only while this gun is BOTH
    // equipped and the active weapon. Damage rides the existing gunMul.
    val fireRateMul: Float = 1f,  // shot-cooldown multiplier (< 1 = faster 連射)
    val reloadMul: Float = 1f,    // reload-time multiplier (< 1 = faster 装填)
    val blastMul: Float = 1f,     // explosion/blast radius multiplier (grenade detonation, beam crater)
    val wallDmgMul: Float = 1f,   // bullet damage vs destructible blocks (ブロックの壊しやすさ)
    // v2.40 gun variants: per-gun ballistics — spread, muzzle velocity, and homing shots
    val spreadMul: Float = 1f,      // bullet spread multiplier (拡散範囲)
    val bulletSpeedMul: Float = 1f, // muzzle-velocity multiplier (弾速)
    val homing: Float = 0f,         // rad/s turn toward the nearest enemy (0 = straight; 追尾弾)
    // THRUSTER
    val thrusterClass: ThrusterClass = ThrusterClass.STANDARD,
    val accelMul: Float = 1f,   // thrust ramp multiplier
    val cruiseMul: Float = 1f,  // cruise-cap multiplier
    // ARMOR / ACCESSORY defence
    val damageMul: Float = 1f,  // incoming damage multiplier (< 1 protects)
    // MELEE_WEAPON
    val meleeDmgMul: Float = 1f,
    val meleeReachMul: Float = 1f,
    val meleeArcMul: Float = 1f,      // v2.39: swing-arc multiplier (1 = the classic 180°)
    val meleeWave: Boolean = false,   // v2.39: the swing throws a flying slash wave
    val meleeKbMul: Float = 1f,       // v2.42: knockback multiplier (>1 sends them flying, <1 barely pushes)
    val meleeLifesteal: Float = 0f,   // v2.42: fraction of dealt melee damage returned as HP
    // General stat multipliers (armor may carry a speed penalty; accessories carry small boons)
    val moveMul: Float = 1f,
    val staRegenMul: Float = 1f,
    val gunMul: Float = 1f,
    // CONSUMABLE (v2.34): what using it does + how strongly (HP healed, blocks gained, buff seconds…)
    val consume: ConsumeKind = ConsumeKind.NONE,
    val power: Float = 0f,
    // Special effects (v2.35): on/off traits + passive HP regeneration per second while equipped
    val traits: Set<ItemTrait> = emptySet(),
    val hpRegen: Float = 0f,
    // LORE (v2.34): the readable text — newline-separated lines shown in the inventory's reading view
    val lore: String = "",
)

/**
 * The player's seven equipment slots. Pure: systems read the aggregate multipliers each tick
 * (a handful of float multiplies); equip/swap goes through [set] so slot/kind compatibility
 * is enforced in exactly one place.
 */
class Loadout(
    var thruster: ItemDef? = null,
    var armor: ItemDef? = null,
    var ranged: ItemDef? = null,
    var melee: ItemDef? = null,
    val accessories: Array<ItemDef?> = arrayOfNulls(3),
) {
    fun get(slot: EquipSlot): ItemDef? = when (slot) {
        EquipSlot.THRUSTER -> thruster
        EquipSlot.ARMOR -> armor
        EquipSlot.RANGED -> ranged
        EquipSlot.MELEE -> melee
        EquipSlot.ACC1 -> accessories[0]
        EquipSlot.ACC2 -> accessories[1]
        EquipSlot.ACC3 -> accessories[2]
    }

    /** Equip [item] (or null to unequip) into [slot]; returns what was there. Rejects a wrong kind. */
    fun set(slot: EquipSlot, item: ItemDef?): ItemDef? {
        require(item == null || compatible(slot, item.kind)) { "cannot equip ${item?.kind} into $slot" }
        val prev = get(slot)
        when (slot) {
            EquipSlot.THRUSTER -> thruster = item
            EquipSlot.ARMOR -> armor = item
            EquipSlot.RANGED -> ranged = item
            EquipSlot.MELEE -> melee = item
            EquipSlot.ACC1 -> accessories[0] = item
            EquipSlot.ACC2 -> accessories[1] = item
            EquipSlot.ACC3 -> accessories[2] = item
        }
        return prev
    }

    fun equipped(): List<ItemDef> = listOfNotNull(thruster, armor, ranged, melee, *accessories)

    // --- Aggregates read by the systems (cheap; a few multiplies) ---
    val hasOverclockThruster: Boolean get() = thruster?.thrusterClass == ThrusterClass.OC
    val thrustAccelMul: Float get() = (thruster?.accelMul ?: 1f)
    val thrustCruiseMul: Float get() = (thruster?.cruiseMul ?: 1f)
    val damageTakenMul: Float get() = equipped().fold(1f) { a, i -> a * i.damageMul }
    val moveMul: Float get() = equipped().fold(1f) { a, i -> a * i.moveMul }
    val staRegenMul: Float get() = equipped().fold(1f) { a, i -> a * i.staRegenMul }
    val gunMul: Float get() = equipped().fold(1f) { a, i -> a * i.gunMul }
    val meleeDmgMul: Float get() = (melee?.meleeDmgMul ?: 1f)
    val meleeReachMul: Float get() = (melee?.meleeReachMul ?: 1f)
    val meleeArcMul: Float get() = (melee?.meleeArcMul ?: 1f)
    val meleeWave: Boolean get() = melee?.meleeWave == true
    val meleeKbMul: Float get() = (melee?.meleeKbMul ?: 1f)
    val meleeLifesteal: Float get() = (melee?.meleeLifesteal ?: 0f)
    // v2.35 special effects: any equipped piece carrying the trait grants it; regen stacks additively.
    fun has(trait: ItemTrait): Boolean = equipped().any { trait in it.traits }
    val hpRegen: Float get() = equipped().fold(0f) { a, i -> a + i.hpRegen }

    companion object {
        fun compatible(slot: EquipSlot, kind: ItemKind): Boolean = when (slot) {
            EquipSlot.THRUSTER -> kind == ItemKind.THRUSTER
            EquipSlot.ARMOR -> kind == ItemKind.ARMOR
            EquipSlot.RANGED -> kind == ItemKind.RANGED_WEAPON
            EquipSlot.MELEE -> kind == ItemKind.MELEE_WEAPON
            EquipSlot.ACC1, EquipSlot.ACC2, EquipSlot.ACC3 -> kind == ItemKind.ACCESSORY
        }
    }
}

/** OC thruster full throttle (v2.33): 3× thrust, 2× cruise — at 2× the stamina bill. */
object FullThrottle {
    const val ACCEL_MUL = 3f
    const val CRUISE_MUL = 2f
    const val DRAIN_MUL = 2f // stamina costs double while the throttle is wide open
}
