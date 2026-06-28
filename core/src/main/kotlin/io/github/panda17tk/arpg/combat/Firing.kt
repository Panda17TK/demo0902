package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.math.Rng

/** Pure spread resolver (legacy combat.js shooting). Each pellet jitters within ±spread of aim. */
object Firing {
    fun bulletAngles(aim: Float, spread: Float, pellets: Int, rng: Rng): FloatArray =
        FloatArray(pellets) { aim + (rng.nextFloat() - 0.5f) * spread * 2f }
}
