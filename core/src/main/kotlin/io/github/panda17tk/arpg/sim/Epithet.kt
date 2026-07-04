package io.github.panda17tk.arpg.sim

/**
 * v2.50 呼び名: what the planet network, in aggregate, has started calling this visitor.
 * Pure — reads the remembered societies and returns one title. The worst deed outranks
 * everything (the network never buries a child's death under later kindness).
 */
object Epithet {
    fun of(memories: Collection<PlanetSocietyState>): String = when {
        memories.any { it.childKilled } -> "星喰い"
        memories.count { it.leaderDefeated } >= 2 -> "王殺し"
        memories.count { it.relicClaimed } >= 2 -> "遺物持ち"
        memories.count { it.mercy >= 0.5f } >= 2 -> "星還し"
        memories.count { it.apexKilled } >= 2 -> "獣狩り"
        memories.size >= 3 -> "巡回者"
        else -> "異邦人"
    }
}

/**
 * v2.50 同期汚染: the wave counter reread as sector degradation (the reinterpretation, not a
 * removal — the pressure system stays; the fiction changes). Stability decays as the broken
 * preservation network's surges deepen, and never quite reaches zero: the machines hold on.
 */
object DesyncGauge {
    fun stability(wave: Int): Int = (100 - (wave - 1) * 3).coerceAtLeast(5)
}
