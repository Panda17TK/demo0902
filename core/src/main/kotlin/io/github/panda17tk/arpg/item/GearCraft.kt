package io.github.panda17tk.arpg.item

import kotlin.math.pow

/**
 * v2.47 武器合成: two identical weapons hone into a "+1" (up to +3). Honed defs are DERIVED,
 * not catalogued — the id carries the level ("gun_mg+2"), and ItemCatalog.byId resolves it back
 * through [leveled], so saves keep working and the catalog stays small.
 */
object GearCraft {
    const val MAX_LEVEL = 3
    private const val SEP = "+"
    private const val DMG_PER_LEVEL = 1.12f    // damage per hone level (guns and blades alike)
    private const val FIRE_PER_LEVEL = 0.96f   // shot cooldown per level (slightly faster)
    private const val RELOAD_PER_LEVEL = 0.94f // reload time per level

    fun baseId(id: String): String = id.substringBefore(SEP)
    fun level(id: String): Int = id.substringAfter(SEP, "0").toIntOrNull()?.coerceIn(0, MAX_LEVEL) ?: 0

    /** Only weapons hone, and only below the cap. */
    fun craftable(def: ItemDef): Boolean =
        (def.kind == ItemKind.RANGED_WEAPON || def.kind == ItemKind.MELEE_WEAPON) && level(def.id) < MAX_LEVEL

    /** The def one hone level above [def] (same family, sharpened numbers). */
    fun honed(def: ItemDef): ItemDef {
        val base = ItemCatalog.byId(baseId(def.id)) ?: def
        return leveled(base, level(def.id) + 1)
    }

    /** [base] raised to hone level [n]: multiplicative per level, name gains a "+n" mark. */
    fun leveled(base: ItemDef, n: Int): ItemDef {
        val k = n.coerceIn(0, MAX_LEVEL)
        if (k == 0) return base
        val kk = k.toFloat()
        return base.copy(
            id = base.id + SEP + k,
            name = base.name + "+$k",
            gunMul = base.gunMul * DMG_PER_LEVEL.pow(kk),
            fireRateMul = base.fireRateMul * FIRE_PER_LEVEL.pow(kk),
            reloadMul = base.reloadMul * RELOAD_PER_LEVEL.pow(kk),
            meleeDmgMul = base.meleeDmgMul * DMG_PER_LEVEL.pow(kk),
        )
    }
}
