package io.github.panda17tk.arpg.save

import com.badlogic.gdx.Gdx
import io.github.panda17tk.arpg.config.WorkshopBoons

/**
 * v2.90 保守員の工房 — the persistent ledger: recovered fragments (the bank) and the ranks
 * bought with them. Thin, defensive preferences IO; every rule lives in [WorkshopCatalog].
 */
object Workshop {
    private const val PREFS = "drift-workshop"

    var bank = 0
        private set
    private val ranks = HashMap<String, Int>()

    fun load() {
        try {
            val p = Gdx.app.getPreferences(PREFS)
            bank = p.getInteger("bank", 0)
            for (item in WorkshopCatalog.ITEMS) {
                ranks[item.id] = p.getInteger("rank.${item.id}", 0).coerceIn(0, item.maxRank)
            }
        } catch (_: Throwable) { /* headless / first boot → empty ledger */ }
    }

    fun rank(id: String): Int = ranks[id] ?: 0

    /** A fallen run's salvage reaches the ledger (no-op for nothing). */
    fun deposit(n: Int) {
        if (n <= 0) return
        bank += n
        persist()
    }

    /** Buy one rank of [id]; false when maxed or unaffordable. */
    fun buy(id: String): Boolean {
        val item = WorkshopCatalog.byId(id) ?: return false
        val r = rank(id)
        if (r >= WorkshopCatalog.rankCap(item, Endings.clears)) return false // v2.104: 周回の印 paces itself
        val c = WorkshopCatalog.cost(item, r)
        if (bank < c) return false
        bank -= c
        ranks[id] = r + 1
        persist()
        return true
    }

    fun boons(): WorkshopBoons = WorkshopCatalog.boonsFor(ranks)

    private fun persist() {
        try {
            val p = Gdx.app.getPreferences(PREFS)
            p.putInteger("bank", bank)
            for ((id, r) in ranks) p.putInteger("rank.$id", r)
            p.flush()
        } catch (_: Throwable) { /* best-effort */ }
    }
}
