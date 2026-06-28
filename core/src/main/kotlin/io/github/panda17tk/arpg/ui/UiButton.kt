package io.github.panda17tk.arpg.ui

/**
 * Pure dp rectangle (HUD space: y-up, origin bottom-left) with an optional label.
 * No libGDX deps → unit-testable. Used for every modal button and upgrade card so that
 * drawing (render/Hud) and hit-testing (GameScreen tap wiring) share one source of truth.
 */
data class UiButton(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val label: String = "",
) {
    val centerX: Float get() = x + w / 2f
    val centerY: Float get() = y + h / 2f

    /** Inclusive on all edges so a tap exactly on the border still counts as a hit. */
    fun contains(px: Float, py: Float): Boolean =
        px >= x && px <= x + w && py >= y && py <= y + h
}
