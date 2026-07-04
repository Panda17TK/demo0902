package io.github.panda17tk.arpg.sim

/**
 * v2.52 星間同期復旧度: how much of the broken interstellar network this drifter has knit back
 * together — the run's TRUE progression axis (the surge counter measures pressure; this measures
 * repair). Pure and derived: jumping systems, visiting planets, and earning their trust all count.
 * Never reaches 100 — the last percent belonged to the people who could read the specifications.
 */
object SyncRestoration {
    fun percent(system: Int, memories: Collection<PlanetSocietyState>): Int {
        val visited = memories.size
        val trusted = memories.count { it.mercy >= 0.5f }
        return ((system - 1) * 12 + visited * 5 + trusted * 8).coerceIn(0, 99)
    }

    /** At 60%+ the gates recognize the keeper's signature and ask one shard less. */
    fun gateShardsNeeded(percent: Int): Int = if (percent >= GATE_DISCOUNT_AT) 2 else 3

    const val GATE_DISCOUNT_AT = 60
}
