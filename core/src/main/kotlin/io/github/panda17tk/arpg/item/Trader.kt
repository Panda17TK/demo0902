package io.github.panda17tk.arpg.item

import io.github.panda17tk.arpg.math.Rng

/** v2.100 行商船: what the vessel sells — supplies, one piece of gear, sometimes a gate shard. */
enum class TraderGoodKind { MED, AMMO, GEAR, SHARD }

data class TraderGood(val kind: TraderGoodKind, val item: ItemDef?, val price: Int, val label: String)

/**
 * 行商船 (v2.100): a friendly vessel adrift in some star systems — the void's answer to the
 * planet markets, and the run's mid-flight dust sink. Pure logic: the stock is deterministic
 * per system seed (3..4 slots — supplies, one random piece of gear, and at times a gate key
 * shard at a steep price), so a revisit under the same sky meets the same shelves.
 */
object Trader {
    /** The system's shelves: 回復キット + 弾薬箱 + one gear pick (+ sometimes a shard). */
    fun stockFor(seed: Long): List<TraderGood> {
        val rng = Rng(seed xor SALT)
        val pool = ItemCatalog.ALL.filter { it.kind != ItemKind.LORE }
        val gear = pool[rng.nextInt(pool.size)]
        val goods = mutableListOf(
            TraderGood(TraderGoodKind.MED, null, MED_PRICE, "回復キット"),
            TraderGood(TraderGoodKind.AMMO, null, AMMO_PRICE, "弾薬箱"),
            TraderGood(TraderGoodKind.GEAR, gear, (Market.priceFor(gear, 0f) * GEAR_MARKUP).toInt().coerceAtLeast(1), gear.name),
        )
        if (rng.nextFloat() < SHARD_CHANCE) {
            goods.add(TraderGood(TraderGoodKind.SHARD, null, SHARD_PRICE, "ゲート鍵の断片"))
        }
        return goods
    }

    const val MED_PRICE = 30
    const val MED_HEAL = 60
    const val AMMO_PRICE = 25
    /** 弾薬箱: half a starting loadout of every reserve. */
    const val AMMO9 = 48
    const val AMMO12 = 12
    const val AMMO_BEAM = 3
    const val AMMO_NADE = 2
    const val SHARD_PRICE = 150 // 高額 — the shortcut costs what a wave train pays
    const val SHARD_CHANCE = 0.5f
    private const val GEAR_MARKUP = 1.2f // no gratitude discount out here
    private const val SALT = 0x71A_DE72L
}
