package io.github.panda17tk.arpg.ui

import kotlin.math.max
import kotlin.math.min

/**
 * v2.88 ボスHPバー — a heavy in range puts its name and health on a top-center bar that slides
 * in, tracks the fight, and slides away when the heavy falls or the drifter leaves. Pure timing
 * and value state; GameScreen scans for the target and draws the band.
 */
class BossBar {
    var name = ""
        private set
    var frac = 1f
        private set

    /** Slide progress: 0 hidden → 1 fully shown. */
    var k = 0f
        private set

    /** Feed the frame's target (or absence). The displayed hp eases toward the real value. */
    fun update(present: Boolean, targetName: String?, targetFrac: Float, dt: Float) {
        if (present && targetName != null) {
            if (name != targetName) frac = targetFrac // a new heavy snaps, no cross-fade
            name = targetName
            frac += (targetFrac.coerceIn(0f, 1f) - frac) * min(1f, dt * EASE)
            k = min(1f, k + dt / SLIDE_IN)
        } else {
            k = max(0f, k - dt / SLIDE_OUT)
        }
    }

    val visible: Boolean get() = k > 0f

    companion object {
        const val SLIDE_IN = 0.25f
        const val SLIDE_OUT = 0.35f
        private const val EASE = 10f
    }
}
