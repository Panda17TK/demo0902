package io.github.panda17tk.arpg.sim

/**
 * v2.87 記憶核の共鳴 — every [PERIOD] seconds the core lets out one slow pulse: a ring that
 * sweeps [SWEEP] seconds out to [RANGE], briefly lighting whatever it passes (the renderer
 * highlights pickups riding the ring). Pure clock → radius; no sim state, no randomness.
 */
object Resonance {
    const val PERIOD = 16f  // seconds between pulses
    const val SWEEP = 4f    // seconds a pulse takes to cross its range
    const val RANGE = 520f  // world px the ring reaches
    const val BAND = 26f    // half-thickness of the lit band

    /** The pulse ring's radius at clock [t], or null while the core rests between pulses. */
    fun radius(t: Float): Float? {
        if (t < 0f) return null
        val phase = t % PERIOD
        return if (phase < SWEEP) phase / SWEEP * RANGE else null
    }

    /** Whether a point [dist] from the core rides the ring at clock [t]. */
    fun lit(t: Float, dist: Float): Boolean {
        val r = radius(t) ?: return false
        return dist >= r - BAND && dist <= r + BAND
    }
}
