package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng

/**
 * v2.91 星系の個性 — every system past the first has a temperament, drawn deterministically
 * from its seed. The trait colors the waves (fill), the economy (dust), the pull of planets
 * (gravity) and the surge pressure (quota). Pure rules; the surge/gravity systems consult them.
 */
enum class SystemTrait(val label: String, val line: String) {
    NONE("", ""),
    STORMY("嵐の星系", "この星系は嵐が絶えない — 敵は荒ぶり、星屑は多くこぼれる"),
    HEAVY("重い星系", "この星系は重い — 惑星の引力が強く働く"),
    CUSTODIAL("保守の星系", "この星系は保守機構が濃い — 清掃の巡回が絶えない"),
    RICH("豊かな星系", "この星系は豊かだ — 星屑は多いが、汚染も深い"),
}

object SystemTraits {
    /** The first system is always calm (the teaching sky); later ones draw a temperament. */
    fun traitFor(systemSeed: Long): SystemTrait {
        if (systemSeed <= 1L) return SystemTrait.NONE
        val pool = listOf(SystemTrait.STORMY, SystemTrait.HEAVY, SystemTrait.CUSTODIAL, SystemTrait.RICH)
        return pool[Rng(systemSeed * -0x61c8864680b583ebL).nextInt(pool.size)]
    }

    /** A quiet wave inherits the system's weather; scheduled events always win. */
    fun fillEvent(trait: SystemTrait, base: WaveEvent, wave: Int): WaveEvent = when {
        base != WaveEvent.NONE -> base
        trait == SystemTrait.STORMY -> WaveEvent.STORM
        trait == SystemTrait.CUSTODIAL && wave % 3 == 1 -> WaveEvent.PURGE
        else -> base
    }

    /** RICH runs deeper contamination: the surge asks for a fifth more bodies. */
    fun quotaMul(trait: SystemTrait): Float = if (trait == SystemTrait.RICH) 1.2f else 1f

    /** RICH kills shed one extra dust pile on top of whatever the wave already grants. */
    fun dustBonus(trait: SystemTrait): Int = if (trait == SystemTrait.RICH) 1 else 0

    /** HEAVY planets pull half again as hard. */
    fun gravityMul(trait: SystemTrait): Float = if (trait == SystemTrait.HEAVY) 1.5f else 1f
}
