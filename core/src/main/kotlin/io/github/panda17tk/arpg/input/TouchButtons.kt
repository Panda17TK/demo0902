package io.github.panda17tk.arpg.input

/**
 * Pure contextual visibility for the on-screen action buttons (P3). Dash / melee / weapon are
 * always shown; reload appears only when the current weapon's magazine is not full; wall appears
 * only when the player actually has materials. Keeps the right-hand cluster small and cuts mis-taps.
 */
object TouchButtons {
    fun visible(blocks: Int, mag: Int, magSize: Int?, canLand: Boolean, hasOverclock: Boolean = false, tuneMode: Boolean = false): Set<TouchButton> {
        val out = linkedSetOf(TouchButton.DASH, TouchButton.MELEE, TouchButton.WEAPON, TouchButton.INV)
        if (tuneMode) out += TouchButton.TUNE // v2.98 調整モード: the tuning door, left of 持物
        if (magSize != null && mag < magSize) out += TouchButton.RELOAD
        if (blocks > 0) out += TouchButton.WALL
        if (canLand) out += TouchButton.LAND // only near a landable planet (space) or on the escape pad (surface)
        if (hasOverclock) out += TouchButton.FULL // full throttle exists only with an OC thruster (v2.33)
        return out
    }
}
