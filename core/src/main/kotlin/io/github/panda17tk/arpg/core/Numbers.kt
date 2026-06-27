package io.github.panda17tk.arpg.core

/**
 * Clamp [v] into [lo, hi]. Returns [def] when [v] is NaN/Infinite.
 * Mirrors the legacy save-validation helper clampNum() so corrupt/tampered
 * save & config JSON can be coerced into safe ranges (see spec §9).
 */
fun clamp(v: Double, lo: Double, hi: Double, def: Double): Double {
    if (!v.isFinite()) return def
    return maxOf(lo, minOf(hi, v))
}
