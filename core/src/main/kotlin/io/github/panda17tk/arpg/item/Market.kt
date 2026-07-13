package io.github.panda17tk.arpg.item

import io.github.panda17tk.arpg.math.Rng

/**
 * 惑星の市 (v2.43): a friendly planet's people will trade. Pure logic — the stock is deterministic
 * per planet, prices scale with the item's class, and the planet's FEELINGS set the terms:
 * gratitude discounts, hostility closes the stalls entirely. The Living Planets reputation finally
 * pays (or refuses) in 星屑.
 */
object Market {
    /** A hostile world will not trade with you. (hostility ≥ 0.5 closes the market.) */
    fun isOpen(hostility: Float): Boolean = hostility < 0.5f

    /** The planet's stock: [SLOTS] deterministic picks (no lore — stories are found, not sold). */
    fun stockFor(planetId: Long): List<ItemDef> {
        val rng = Rng(planetId xor SALT)
        val pool = ItemCatalog.ALL.filter { it.kind != ItemKind.LORE && !it.ngPlusOnly } // v2.186: the NG+ rail is never sold
        return List(SLOTS) { pool[rng.nextInt(pool.size)] }
    }

    /** Price in 星屑: a base by class, +50% for special traits, then the gratitude discount (up to 40%). */
    fun priceFor(item: ItemDef, mercy: Float): Int {
        val base = when (item.kind) {
            ItemKind.CONSUMABLE -> 18
            ItemKind.ACCESSORY -> 50
            ItemKind.RANGED_WEAPON, ItemKind.MELEE_WEAPON -> 65
            ItemKind.THRUSTER, ItemKind.ARMOR -> 80
            ItemKind.LORE -> 10
        }
        val special = if (item.traits.isNotEmpty() || item.thrusterClass == ThrusterClass.OC) 1.5f else 1f
        val discount = 1f - 0.4f * mercy.coerceIn(0f, 1f)
        return (base * special * discount).toInt().coerceAtLeast(1)
    }

    const val SLOTS = 6
    private const val SALT = 0x3A_7B_11_22L
}
