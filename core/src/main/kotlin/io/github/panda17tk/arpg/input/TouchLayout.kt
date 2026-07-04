package io.github.panda17tk.arpg.input

import kotlin.math.cos
import kotlin.math.sin

enum class TouchButton { FIRE, MELEE, DASH, RELOAD, WALL, WEAPON, LAND, INV, FULL }

/**
 * Pure geometry + hit-testing for the on-screen touch controls (no libGDX deps → unit-testable).
 * Coordinates are HUD space: y-up, origin bottom-left. Left half drives the move stick; the
 * right side holds the action cluster.
 *
 * v2.55 hub-and-arc layout: DASH is a LARGE hub tucked into the bottom-right corner; 近接/武器/
 * 装填 ride its inner arc and 壁/全開 the outer arc (a thumb sweep covers everything). 持物 (INV)
 * docks top-right under the pause button, out of combat's way. Everything is computed inward
 * from the screen edges, so no button clips or crowds on any aspect ratio.
 */
class TouchLayout(var screenW: Float = 0f, var screenH: Float = 0f) {
    /** v2.56: user overrides from the layout editor — fraction centres + size multipliers.
     *  Applied on top of the default hub-and-arc geometry; positions clamp into the right
     *  (non-stick) zone and onto the screen, so a saved layout can never strand a button. */
    var tweaks: Map<TouchButton, ButtonTweak> = emptyMap()

    private val minDim get() = minOf(screenW, screenH)
    val stickRadius get() = minDim * 0.14f
    val stickCx get() = screenW * 0.20f
    val stickCy get() = screenH * 0.16f
    val buttonRadius get() = minDim * 0.075f
    val dashRadius get() = minDim * 0.115f // the hub — the button under the thumb all game long

    // P3: fixed aim-guide ring — moved up-left of the arc (v2.55) so it never kisses a button.
    val aimGuideCx get() = screenW * 0.52f
    val aimGuideCy get() = screenH * 0.30f
    val aimGuideRadius get() = minDim * 0.10f

    private val pad get() = minDim * 0.030f
    private val hubX get() = screenW - pad - dashRadius
    private val hubY get() = pad + dashRadius
    private val orbit1 get() = dashRadius + buttonRadius + minDim * 0.028f
    private val orbit2 get() = orbit1 + 2f * buttonRadius + minDim * 0.022f

    private fun arcX(deg: Float, r: Float) = hubX + cos(Math.toRadians(deg.toDouble())).toFloat() * r
    private fun arcY(deg: Float, r: Float) = hubY + sin(Math.toRadians(deg.toDouble())).toFloat() * r

    private val order = listOf(
        TouchButton.DASH, TouchButton.MELEE, TouchButton.WEAPON, TouchButton.RELOAD,
        TouchButton.WALL, TouchButton.FULL, TouchButton.INV, TouchButton.LAND,
    )

    fun all(): List<TouchButton> = order

    private fun baseCenterX(b: TouchButton): Float = when (b) {
        TouchButton.DASH -> hubX
        TouchButton.MELEE -> arcX(180f, orbit1)   // inner arc, thumb-nearest first
        TouchButton.WEAPON -> arcX(135f, orbit1)
        TouchButton.RELOAD -> arcX(90f, orbit1)
        TouchButton.FULL -> arcX(157.5f, orbit2)  // outer arc
        TouchButton.WALL -> arcX(112.5f, orbit2)
        TouchButton.INV -> screenW - pad - buttonRadius // top-right dock
        TouchButton.LAND -> screenW * 0.62f // contextual: big target where the eye already is
        TouchButton.FIRE -> screenW * 0.62f // legacy id (fire lives on the aim stick)
    }

    private fun baseCenterY(b: TouchButton): Float = when (b) {
        TouchButton.DASH -> hubY
        TouchButton.MELEE -> arcY(180f, orbit1)
        TouchButton.WEAPON -> arcY(135f, orbit1)
        TouchButton.RELOAD -> arcY(90f, orbit1)
        TouchButton.FULL -> arcY(157.5f, orbit2)
        TouchButton.WALL -> arcY(112.5f, orbit2)
        TouchButton.INV -> screenH - 130f - buttonRadius - 10f // just under the top HUD band
        TouchButton.LAND -> screenH * 0.82f
        TouchButton.FIRE -> screenH * 0.18f
    }

    /** Left half of the screen is the movement-stick zone. */
    fun isInStickZone(x: Float, y: Float): Boolean = x < screenW * 0.45f

    private fun baseRadius(b: TouchButton): Float = when (b) {
        TouchButton.DASH -> dashRadius
        TouchButton.LAND -> buttonRadius * 1.8f // a big, obvious "land here" target (v2.34)
        else -> buttonRadius
    }

    fun radiusOf(b: TouchButton): Float = baseRadius(b) * (tweaks[b]?.scale ?: 1f)

    fun centerX(b: TouchButton): Float {
        val t = tweaks[b] ?: return baseCenterX(b)
        val r = radiusOf(b)
        // Stay tappable: inside the screen AND inside the right (non-stick) zone.
        return (t.fx * screenW).coerceIn(maxOf(r, screenW * 0.46f + r), screenW - r)
    }

    fun centerY(b: TouchButton): Float {
        val t = tweaks[b] ?: return baseCenterY(b)
        val r = radiusOf(b)
        return (t.fy * screenH).coerceIn(r, screenH - r)
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
