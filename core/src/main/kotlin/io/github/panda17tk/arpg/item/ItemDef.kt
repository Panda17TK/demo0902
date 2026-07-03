package io.github.panda17tk.arpg.item

/** The seven equipment slots (v2.33): thruster / armor / ranged / melee / three accessories. */
enum class EquipSlot { THRUSTER, ARMOR, RANGED, MELEE, ACC1, ACC2, ACC3 }

/** What an item IS — which slot family it can occupy. CONSUMABLE is used from the ITEMS tab;
 *  LORE (読み物 — v2.34) is a readable that deepens the world and is never equipped or spent. */
enum class ItemKind { THRUSTER, ARMOR, RANGED_WEAPON, MELEE_WEAPON, ACCESSORY, CONSUMABLE, LORE }

/** Thruster classes: OC (overclocked) thrusters unlock the full-throttle mode. */
enum class ThrusterClass { STANDARD, OC }

/** What using a CONSUMABLE does (v2.34). The magnitude rides in [ItemDef.power]. */
enum class ConsumeKind { NONE, HEAL, STAMINA, STAMINA_INF, DASH_UP, SMOKE, BLOCKS, AMMO_ALL }

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
    // THRUSTER
    val thrusterClass: ThrusterClass = ThrusterClass.STANDARD,
    val accelMul: Float = 1f,   // thrust ramp multiplier
    val cruiseMul: Float = 1f,  // cruise-cap multiplier
    // ARMOR / ACCESSORY defence
    val damageMul: Float = 1f,  // incoming damage multiplier (< 1 protects)
    // MELEE_WEAPON
    val meleeDmgMul: Float = 1f,
    val meleeReachMul: Float = 1f,
    // General stat multipliers (armor may carry a speed penalty; accessories carry small boons)
    val moveMul: Float = 1f,
    val staRegenMul: Float = 1f,
    val gunMul: Float = 1f,
    // CONSUMABLE (v2.34): what using it does + how strongly (HP healed, blocks gained, buff seconds…)
    val consume: ConsumeKind = ConsumeKind.NONE,
    val power: Float = 0f,
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
