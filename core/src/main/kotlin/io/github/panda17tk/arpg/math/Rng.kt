package io.github.panda17tk.arpg.math

import kotlin.random.Random

/**
 * Seedable deterministic RNG used by all gameplay randomness so runs/saves
 * reproduce (spec §12). Wraps kotlin.random.Random for a stable algorithm.
 */
class Rng(seed: Long) {
    private val random = Random(seed)
    fun nextFloat(): Float = random.nextFloat()           // [0,1)
    fun nextInt(untilExclusive: Int): Int = random.nextInt(untilExclusive)
    fun range(min: Float, max: Float): Float = min + (max - min) * random.nextFloat()
}
