package io.github.panda17tk.arpg.core

/** App-wide constants. Simulation timing mirrors the legacy fixed-step loop (spec §7). */
object Constants {
    const val APP_TITLE = "drift"
    const val VERSION_NAME = "2.0.0"

    /** Fixed simulation timestep (seconds). */
    const val FIXED_DT = 1.0f / 60.0f
    /** Maximum frame delta before clamping (prevents tunneling on stalls). */
    const val MAX_DT = 0.05f
    /** Max simulation sub-steps per frame (anti death-spiral). v2.165: 5→3 — on a phone that
     *  falls behind, five catch-up ticks per frame amplified the very load that caused the
     *  fall; three lets the game breathe (it slows slightly instead of stuttering harder). */
    const val MAX_STEPS = 3
}
