package io.github.panda17tk.arpg.sim

/**
 * Pure enemy progression math. Mobs gain XP for killing other mobs (tribe brawls), and levelling
 * up grows **skill variety** (how many of the archetype's attacks are usable) and **smarts** (for
 * tactical AI) — but NOT base damage. Higher-level killers earn more XP per kill.
 */
object Leveling {
    const val BASE_XP_PER_KILL = 10f

    fun xpForKill(killerLevel: Int): Float = BASE_XP_PER_KILL * (1f + 0.5f * (killerLevel - 1).coerceAtLeast(0))

    fun threshold(level: Int): Float = 30f * level.coerceAtLeast(1)

    fun attacksForLevel(level: Int, maxAttacks: Int): Int =
        if (maxAttacks <= 0) 0 else minOf(maxAttacks, level.coerceAtLeast(1))

    fun smarts(intelligence: Float, level: Int): Float = intelligence + 0.1f * (level - 1).coerceAtLeast(0)
}
