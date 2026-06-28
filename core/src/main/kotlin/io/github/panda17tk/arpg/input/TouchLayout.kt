package io.github.panda17tk.arpg.input

enum class TouchButton { FIRE, MELEE, DASH, RELOAD, WALL, WEAPON }

/**
 * Pure geometry + hit-testing for the on-screen touch controls (no libGDX deps → unit-testable).
 * Coordinates are HUD space: y-up, origin bottom-left. Left half drives the move stick; the
 * right half holds the action buttons.
 */
class TouchLayout(var screenW: Float = 0f, var screenH: Float = 0f) {
    private val minDim get() = minOf(screenW, screenH)
    val stickRadius get() = minDim * 0.14f
    val stickCx get() = screenW * 0.20f
    val stickCy get() = screenH * 0.16f
    val buttonRadius get() = minDim * 0.062f

    /** Twin-stick: fire is the right aim stick, so only secondary actions are buttons (top-right). */
    private val buttons = listOf(
        Triple(TouchButton.DASH, 0.93f, 0.88f),
        Triple(TouchButton.MELEE, 0.80f, 0.88f),
        Triple(TouchButton.RELOAD, 0.93f, 0.74f),
        Triple(TouchButton.WALL, 0.80f, 0.74f),
        Triple(TouchButton.WEAPON, 0.865f, 0.60f),
    )

    fun all(): List<TouchButton> = buttons.map { it.first }
    fun centerX(b: TouchButton): Float = buttons.first { it.first == b }.second * screenW
    fun centerY(b: TouchButton): Float = buttons.first { it.first == b }.third * screenH

    /** Left half of the screen is the movement-stick zone. */
    fun isInStickZone(x: Float, y: Float): Boolean = x < screenW * 0.45f

    /** Which action button (if any) contains the point. Returns null inside the stick zone. */
    fun button(x: Float, y: Float): TouchButton? {
        if (x < screenW * 0.45f) return null
        val r2 = buttonRadius * buttonRadius
        for ((b, fx, fy) in buttons) {
            val dx = x - fx * screenW; val dy = y - fy * screenH
            if (dx * dx + dy * dy <= r2) return b
        }
        return null
    }
}
