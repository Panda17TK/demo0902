package io.github.panda17tk.arpg.save

import io.github.panda17tk.arpg.config.WorkshopBoons
import kotlin.math.pow

/** One buyable line in the workshop: a quiet craft, learned in ranks. */
data class WorkshopItem(val id: String, val title: String, val desc: String, val baseCost: Int, val maxRank: Int)

/**
 * v2.90 保守員の工房 — the catalog and its arithmetic, all pure. The impure [Workshop]
 * object owns the ledger (preferences); this object owns what things cost and what they do.
 */
object WorkshopCatalog {
    val ITEMS = listOf(
        WorkshopItem("hull", "外殻の補強", "初期HP +10 / 段", baseCost = 60, maxRank = 3),
        WorkshopItem("mend", "自己修復の学習", "休息回復 +0.5/秒 / 段", baseCost = 100, maxRank = 2),
        WorkshopItem("hands", "装填の手癖", "リロード 5%短縮 / 段", baseCost = 80, maxRank = 3),
        WorkshopItem("breath", "推進の呼吸", "スタミナ +15 / 段", baseCost = 90, maxRank = 2),
        WorkshopItem("eye", "拾集の目", "資材ドロップ +5% / 段", baseCost = 120, maxRank = 2),
        // v2.104 周回の印: the sixth craft — one rank unlocks per completed sync (Endings.clears).
        WorkshopItem("core", "管制核の欠片", "全ステータス +2% / 段", baseCost = 250, maxRank = 3),
    )

    fun byId(id: String): WorkshopItem? = ITEMS.firstOrNull { it.id == id }

    /** v2.104: how many ranks of [item] the account may hold — the core craft is paced by clears. */
    fun rankCap(item: WorkshopItem, clears: Int): Int =
        if (item.id == "core") minOf(item.maxRank, clears.coerceAtLeast(0)) else item.maxRank

    /** The next rank's price doubles each time: 60 → 120 → 240. */
    fun cost(item: WorkshopItem, ownedRank: Int): Int = item.baseCost * (1 shl ownedRank.coerceIn(0, item.maxRank))

    /** What a rank book adds up to at run start. */
    fun boonsFor(ranks: Map<String, Int>): WorkshopBoons = WorkshopBoons(
        hull = 10f * (ranks["hull"] ?: 0),
        regenPerSec = 0.5f * (ranks["mend"] ?: 0),
        reloadMul = 0.95f.pow(ranks["hands"] ?: 0),
        stamina = 15f * (ranks["breath"] ?: 0),
        loot = 0.05f * (ranks["eye"] ?: 0),
        allMul = 1f + 0.02f * (ranks["core"] ?: 0), // v2.104 周回の印
    )

    /** The tithe the workshop recovers from a fallen run's carried fragments. */
    fun salvage(carriedDust: Int): Int = (carriedDust * 0.15f).toInt()
}
