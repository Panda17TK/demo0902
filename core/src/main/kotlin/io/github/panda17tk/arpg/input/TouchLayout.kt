package io.github.panda17tk.arpg.input

import kotlin.math.cos
import kotlin.math.sin

enum class TouchButton { FIRE, MELEE, DASH, RELOAD, WALL, WEAPON, LAND, INV, FULL, TUNE }

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

    /** v2.65 左利き配置: mirror everything — stick right, action cluster bottom-left. All
     *  geometry stays authored right-handed and flips through [mx] at the last moment. */
    var mirrored = false

    private fun mx(x: Float) = if (mirrored) screenW - x else x

    private val minDim get() = minOf(screenW, screenH)
    val stickRadius get() = minDim * 0.14f
    val stickCx get() = mx(screenW * 0.20f)
    val stickCy get() = screenH * 0.16f
    val buttonRadius get() = minDim * 0.075f
    val dashRadius get() = minDim * 0.115f // the hub — the button under the thumb all game long

    // P3: fixed aim-guide ring — moved up-left of the arc (v2.55) so it never kisses a button.
    val aimGuideCx get() = mx(screenW * 0.52f)
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
        TouchButton.TUNE, // v2.98 調整: docked left of 持物 (visible only in tune mode)
    )

    fun all(): List<TouchButton> = order

    /** v2.84: phones are tall — on portrait screens the shipped default is the reference
     *  arrangement (two thumb columns up the right side); the hub-and-arc stays for
     *  landscape/desktop, whose short height can't hold the portrait column. */
    private val portrait get() = screenH >= screenW * 1.75f

    // v2.84 既定配置 (portrait): an edge column (武器/全開/壁/持物) climbing the right rim
    // above the dash hub, and an inner column (装填/近接/着陸) one thumb-length in.
    private fun portraitFx(b: TouchButton): Float = when (b) {
        TouchButton.DASH -> 0.855f
        TouchButton.MELEE -> 0.655f
        TouchButton.WEAPON -> 0.890f
        TouchButton.RELOAD -> 0.600f
        TouchButton.FULL -> 0.905f
        TouchButton.WALL -> 0.890f
        TouchButton.INV -> 0.905f
        TouchButton.TUNE -> 0.735f // v2.98: the 持物 button's left-hand neighbour
        TouchButton.LAND, TouchButton.FIRE -> 0.600f
    }

    private fun portraitFy(b: TouchButton): Float = when (b) {
        TouchButton.DASH -> 0.170f
        TouchButton.MELEE -> 0.235f
        TouchButton.WEAPON -> 0.280f
        TouchButton.RELOAD -> 0.150f
        TouchButton.FULL -> 0.370f
        TouchButton.WALL -> 0.545f
        TouchButton.INV -> 0.710f
        TouchButton.TUNE -> 0.775f // v2.98: 持物's upper-left neighbour (clear of 着陸's big disc)
        TouchButton.LAND, TouchButton.FIRE -> 0.630f
    }

    private fun baseCenterX(b: TouchButton): Float = mx(
        if (portrait) portraitFx(b) * screenW
        else when (b) {
            TouchButton.DASH -> hubX
            TouchButton.MELEE -> arcX(180f, orbit1)   // inner arc, thumb-nearest first
            TouchButton.WEAPON -> arcX(135f, orbit1)
            TouchButton.RELOAD -> arcX(90f, orbit1)
            TouchButton.FULL -> arcX(157.5f, orbit2)  // outer arc
            TouchButton.WALL -> arcX(112.5f, orbit2)
            TouchButton.INV -> screenW - pad - buttonRadius // top-right dock
            TouchButton.TUNE -> screenW - pad - 3f * buttonRadius - 8f // v2.98: 持物's left neighbour
            TouchButton.LAND -> screenW * 0.62f // contextual: big target where the eye already is
            TouchButton.FIRE -> screenW * 0.62f // legacy id (fire lives on the aim stick)
        }
    )

    private fun baseCenterY(b: TouchButton): Float =
        if (portrait) portraitFy(b) * screenH
        else when (b) {
            TouchButton.DASH -> hubY
            TouchButton.MELEE -> arcY(180f, orbit1)
            TouchButton.WEAPON -> arcY(135f, orbit1)
            TouchButton.RELOAD -> arcY(90f, orbit1)
            TouchButton.FULL -> arcY(157.5f, orbit2)
            TouchButton.WALL -> arcY(112.5f, orbit2)
            TouchButton.INV -> screenH - 130f - buttonRadius - 10f // just under the top HUD band
            TouchButton.TUNE -> screenH - 130f - buttonRadius - 10f // v2.98: same shelf
            TouchButton.LAND -> screenH * 0.82f
            TouchButton.FIRE -> screenH * 0.18f
        }

    /** The movement-stick zone: the left half — or the right half when mirrored (v2.65). */
    fun isInStickZone(x: Float, y: Float): Boolean =
        if (mirrored) x > screenW * 0.55f else x < screenW * 0.45f

    private fun baseRadius(b: TouchButton): Float = when (b) {
        TouchButton.DASH -> dashRadius
        TouchButton.LAND -> buttonRadius * 1.8f // a big, obvious "land here" target (v2.34)
        else -> buttonRadius
    }

    fun radiusOf(b: TouchButton): Float = baseRadius(b) * (tweaks[b]?.scale ?: 1f)

    fun centerX(b: TouchButton): Float {
        val t = tweaks[b] ?: return baseCenterX(b)
        val r = radiusOf(b)
        // Stay tappable: inside the screen AND inside the non-stick zone (side flips when mirrored).
        return if (mirrored) {
            (t.fx * screenW).coerceIn(r, minOf(screenW * 0.54f - r, screenW - r))
        } else {
            (t.fx * screenW).coerceIn(maxOf(r, screenW * 0.46f + r), screenW - r)
        }
    }

    fun centerY(b: TouchButton): Float {
        val t = tweaks[b] ?: return baseCenterY(b)
        val r = radiusOf(b)
        return (t.fy * screenH).coerceIn(r, screenH - r)
    }

    fun button(x: Float, y: Float): TouchButton? {
        if (isInStickZone(x, y)) return null
        for (b in order) {
            val rr = radiusOf(b)
            val dx = x - centerX(b); val dy = y - centerY(b)
            if (dx * dx + dy * dy <= rr * rr) return b
        }
        return null
    }
}
