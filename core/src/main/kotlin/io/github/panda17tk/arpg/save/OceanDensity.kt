package io.github.panda17tk.arpg.save

/**
 * v2.165 海の密度 — how full the space ocean spawns. The 30× sky (v2.144) reads beautifully but
 * costs real phones their frame rate even under the v2.164 LOD, so the density became the
 * player's choice: 高 keeps the full thirtyfold ocean, 中 (the default) roughly a third, 低 a
 * seventh. The spawn rng is always drawn in full — a tier only decides which school members
 * become real entities — so a given seed's ocean is a strict subset of its fuller tiers, and
 * the whales, tyrants and hunters (count-1 spawns) survive every tier. Held in memory here;
 * TitleScreen loads/persists it with the other settings. The sim itself never reads this —
 * the screen hands the stride into WorldFactory like every other world parameter.
 */
object OceanDensity {
    const val LOW = 0
    const val MEDIUM = 1
    const val HIGH = 2

    var tier = MEDIUM

    /** Spawn keep-stride: every n-th member of a school becomes a real entity. */
    fun keep(t: Int = tier): Int = when (t) {
        HIGH -> 1
        MEDIUM -> 3
        else -> 7
    }

    fun label(t: Int = tier): String = when (t) {
        HIGH -> "高"
        MEDIUM -> "中"
        else -> "低"
    }

    /** Taps cycle downward like the volume: 高→中→低→高. */
    fun next(t: Int = tier): Int = (t + 2) % 3
}
