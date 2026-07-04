package io.github.panda17tk.arpg.input

enum class TouchButton { FIRE, MELEE, DASH, RELOAD, WALL, WEAPON, LAND, INV, FULL }

/**
 * Pure geometry + hit-testing for the on-screen touch controls (no libGDX deps → unit-testable).
 * Coordinates are HUD space: y-up, origin bottom-left. Left half drives the move stick; the
 * right half holds the action buttons.
 *
 * v2.54 modern layout: the action cluster is a bottom-right GRID computed from the screen edge
 * inward — fixed safe padding, uniform gaps, larger targets — so no button ever clips off-screen
 * or crowds its neighbour, on any aspect ratio (the old fraction-based spots did both on phones).
 */
class TouchLayout(var screenW: Float = 0f, var screenH: Float = 0f) {
    private val minDim get() = minOf(screenW, screenH)
    val stickRadius get() = minDim * 0.14f
    val stickCx get() = screenW * 0.20f
    val stickCy get() = screenH * 0.16f
    val buttonRadius get() = minDim * 0.075f // v2.54: +21% — thumb-sized targets

    // P3: fixed aim-guide ring on the right — a "rest here to aim" hint, kept clear of the
    // action buttons. The live aim stick still floats to the actual thumb position when active.
    val aimGuideCx get() = screenW * 0.58f
    val aimGuideCy get() = screenH * 0.18f
    val aimGuideRadius get() = minDim * 0.10f

    // Grid metrics: columns march inward from the right edge, rows upward from the bottom.
    private val pad get() = minDim * 0.030f
    private val gap get() = minDim * 0.035f
    private fun colX(i: Int) = screenW - pad - buttonRadius - i * (2f * buttonRadius + gap)
    private fun rowY(i: Int) = pad + buttonRadius + i * (2f * buttonRadius + gap)

    /** Twin-stick: fire is the right aim stick; secondary actions in the bottom-right grid. */
    private val order = listOf(
        TouchButton.DASH, TouchButton.RELOAD, TouchButton.FULL,
        TouchButton.WEAPON, TouchButton.MELEE,
        TouchButton.WALL, TouchButton.INV,
        TouchButton.LAND,
    )

    fun all(): List<TouchButton> = order

    fun centerX(b: TouchButton): Float = when (b) {
        TouchButton.DASH -> colX(0)
        TouchButton.RELOAD -> colX(1)
        TouchButton.FULL -> colX(2)
        TouchButton.WEAPON -> colX(0)
        TouchButton.MELEE -> colX(1)
        TouchButton.WALL -> colX(0)
        TouchButton.INV -> colX(1)
        TouchButton.LAND -> screenW * 0.62f // contextual: big target where the eye already is
        TouchButton.FIRE -> screenW * 0.62f // legacy id (fire lives on the aim stick)
    }

    fun centerY(b: TouchButton): Float = when (b) {
        TouchButton.DASH, TouchButton.RELOAD, TouchButton.FULL -> rowY(0)
        TouchButton.WEAPON, TouchButton.MELEE -> rowY(1)
        TouchButton.WALL, TouchButton.INV -> rowY(2)
        TouchButton.LAND -> screenH * 0.82f
        TouchButton.FIRE -> screenH * 0.18f
    }

    /** Left half of the screen is the movement-stick zone. */
    fun isInStickZone(x: Float, y: Float): Boolean = x < screenW * 0.45f

    /** Dash / weapon / reload are a touch larger for easier tapping; LAND is the landing UX itself. */
    fun radiusOf(b: TouchButton): Float = buttonRadius * when (b) {
        TouchButton.LAND -> 1.8f // a big, obvious "land here" target (v2.34)
        TouchButton.DASH -> 1.12f // v2.54: modest growth — the base radius already grew
        else -> 1f
    }

    fun button(x: Float, y: Float): TouchButton? {
        if (x < screenW * 0.45f) return null
        for (b in order) {
            val rr = radiusOf(b)
            val dx = x - centerX(b); val dy = y - centerY(b)
            if (dx * dx + dy * dy <= rr * rr) return b
        }
        return null
    }
}
