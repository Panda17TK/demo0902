package io.github.panda17tk.arpg.input

enum class TouchButton { FIRE, MELEE, DASH, RELOAD, WALL, WEAPON, LAND }

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

    // P3: fixed aim-guide ring on the right — a "rest here to aim" hint, kept clear of the
    // action buttons. The live aim stick still floats to the actual thumb position when active.
    val aimGuideCx get() = screenW * 0.58f
    val aimGuideCy get() = screenH * 0.18f
    val aimGuideRadius get() = minDim * 0.10f

    /** Twin-stick: fire is the right aim stick; secondary actions in a bottom-right grid (thumb-reach). */
    private val buttons = listOf(
        Triple(TouchButton.DASH, 0.93f, 0.10f),
        Triple(TouchButton.RELOAD, 0.78f, 0.10f),
        Triple(TouchButton.WEAPON, 0.93f, 0.23f),
        Triple(TouchButton.MELEE, 0.78f, 0.23f),
        Triple(TouchButton.WALL, 0.855f, 0.36f),
        Triple(TouchButton.LAND, 0.62f, 0.82f), // contextual: only shown near a planet / on the escape pad
    )

    fun all(): List<TouchButton> = buttons.map { it.first }
    fun centerX(b: TouchButton): Float = buttons.first { it.first == b }.second * screenW
    fun centerY(b: TouchButton): Float = buttons.first { it.first == b }.third * screenH

    /** Left half of the screen is the movement-stick zone. */
    fun isInStickZone(x: Float, y: Float): Boolean = x < screenW * 0.45f

    /** Which action button (if any) contains the point. Returns null inside the stick zone. */
    /** Dash / weapon / reload are a touch larger for easier tapping. */
    fun radiusOf(b: TouchButton): Float = buttonRadius * when (b) {
        TouchButton.LAND -> 1.55f // a big, obvious "land here" target
        TouchButton.DASH, TouchButton.WEAPON, TouchButton.RELOAD -> 1.18f
        else -> 1f
    }

    fun button(x: Float, y: Float): TouchButton? {
        if (x < screenW * 0.45f) return null
        for ((b, fx, fy) in buttons) {
            val rr = radiusOf(b)
            val dx = x - fx * screenW; val dy = y - fy * screenH
            if (dx * dx + dy * dy <= rr * rr) return b
        }
        return null
    }
}
