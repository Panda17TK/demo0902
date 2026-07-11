package io.github.panda17tk.arpg.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.Viewport
import io.github.panda17tk.arpg.input.TouchButton
import io.github.panda17tk.arpg.input.TouchControls
import kotlin.math.hypot
import kotlin.math.min

/**
 * On-screen touch controls (Android): the move stick, the floating aim+fire stick, and the
 * contextually-visible action buttons (P3). Pure painting over input.TouchControls state —
 * GameScreen decides when to draw and what the LAND button says (着陸 vs 発進).
 *
 * v2.54 modern look: each button is a dark glass disc with a function-coloured ring and an
 * auto-fitted label (long labels shrink instead of spilling out of the circle).
 */
object TouchOverlay {
    private val cStickBase = Color(1f, 1f, 1f, 0.06f)
    private val cStickKnob = Color(1f, 1f, 1f, 0.30f)
    private val cAimBase = Color(1f, 0.5f, 0.4f, 0.16f)
    private val cAimKnob = Color(1f, 0.5f, 0.4f, 0.5f)
    private val cAimGuide = Color(1f, 0.5f, 0.4f, 0.06f)
    private val cDisc = Color(0.05f, 0.07f, 0.11f, 0.62f)      // the glass body
    private val cDiscPressed = Color(0.16f, 0.20f, 0.28f, 0.85f)
    private val cLabel = Color(0.92f, 0.95f, 1f, 1f)

    // Function-coloured rings — one hue per verb, so the cluster reads at a glance.
    private val ringOf = mapOf(
        TouchButton.DASH to Color(0.45f, 0.85f, 1f, 0.75f),    // dash: cyan
        TouchButton.RELOAD to Color(0.55f, 0.65f, 1f, 0.70f),  // reload: blue
        TouchButton.FULL to Color(1f, 0.65f, 0.35f, 0.75f),    // full throttle: burn orange
        TouchButton.WEAPON to Color(1f, 0.82f, 0.40f, 0.70f),  // weapon: amber
        TouchButton.MELEE to Color(1f, 0.50f, 0.45f, 0.75f),   // melee/fire: red
        TouchButton.WALL to Color(0.60f, 0.85f, 0.65f, 0.70f), // wall: green-grey
        TouchButton.INV to Color(0.75f, 0.60f, 1f, 0.70f),     // inventory: violet
        TouchButton.LAND to Color(0.35f, 0.95f, 0.50f, 0.90f), // land: THE green button
    )

    fun draw(
        shapes: ShapeRenderer, batch: SpriteBatch, font: BitmapFont, vp: Viewport,
        touch: TouchControls, landLabel: String, swapMeleeFire: Boolean = false,
        showAll: Boolean = false, editTarget: TouchButton? = null, // v2.56 layout editor
    ) {
        val l = touch.layout
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.projectionMatrix = vp.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = cStickBase; shapes.circle(l.stickCx, l.stickCy, l.stickRadius, 28)
        if (touch.stickActive) {
            shapes.color = cStickKnob
            knob(shapes, l.stickCx, l.stickCy, touch.knobX - touch.baseX, touch.knobY - touch.baseY, l.stickRadius)
        }
        // right aim+fire stick — floats at the thumb when active, reddish = firing;
        // otherwise a fixed "rest here to aim" guide ring sits on the right (P3).
        if (touch.aimActive) {
            shapes.color = cAimBase; shapes.circle(touch.aimBaseX, touch.aimBaseY, l.stickRadius, 28)
            shapes.color = cAimKnob
            knob(shapes, touch.aimBaseX, touch.aimBaseY, touch.aimKnobX - touch.aimBaseX, touch.aimKnobY - touch.aimBaseY, l.stickRadius)
        } else {
            shapes.color = cAimGuide; shapes.circle(l.aimGuideCx, l.aimGuideCy, l.aimGuideRadius, 24)
        }
        // Action buttons: ring (function colour) → glass disc → label. Pressed = brighter + grown.
        for (b in l.all()) {
            if (!showAll && b !in touch.visibleButtons) continue
            val pressed = b in touch.pressedButtons
            val r = l.radiusOf(b) * (if (pressed) 1.12f else 1f)
            val cx = l.centerX(b); val cy = l.centerY(b)
            // v2.56: the layout editor's selected button gets a bright halo.
            if (b == editTarget) { shapes.color = cLabel; shapes.circle(cx, cy, r + 5f, 32) }
            val ring = ringOf[b] ?: cLabel
            shapes.color = ring
            shapes.circle(cx, cy, r, 32)
            shapes.color = if (pressed) cDiscPressed else cDisc
            shapes.circle(cx, cy, r - 2.5f, 32)
            pictogram(shapes, b, cx, cy, r, landLabel, swapMeleeFire) // v2.170 文字の消灯
        }
        shapes.end()
    }

    /** v2.170 文字の消灯: wordless button faces — one small pictogram per verb, drawn in the
     *  same filled-shape pass as the disc (no font: nothing to garble, fit, or translate). */
    private fun pictogram(
        shapes: ShapeRenderer, b: TouchButton, cx: Float, cy: Float, r: Float,
        landLabel: String, swapMeleeFire: Boolean,
    ) {
        shapes.color = cLabel
        val s = r * 0.38f
        val fire = b == TouchButton.FIRE || (b == TouchButton.MELEE && swapMeleeFire)
        when {
            fire -> { // reticle: a dot and four ticks
                shapes.circle(cx, cy, s * 0.30f, 12)
                shapes.rect(cx - s, cy - s * 0.10f, s * 0.55f, s * 0.2f)
                shapes.rect(cx + s * 0.45f, cy - s * 0.10f, s * 0.55f, s * 0.2f)
                shapes.rect(cx - s * 0.10f, cy - s, s * 0.2f, s * 0.55f)
                shapes.rect(cx - s * 0.10f, cy + s * 0.45f, s * 0.2f, s * 0.55f)
            }
            b == TouchButton.MELEE -> // the blade: one diagonal slash
                shapes.rectLine(cx - s, cy - s, cx + s, cy + s, s * 0.3f)
            b == TouchButton.DASH -> { // double chevron, running right
                shapes.triangle(cx - s, cy + s * 0.8f, cx - s, cy - s * 0.8f, cx - s * 0.1f, cy)
                shapes.triangle(cx + s * 0.1f, cy + s * 0.8f, cx + s * 0.1f, cy - s * 0.8f, cx + s, cy)
            }
            b == TouchButton.RELOAD -> { // a fresh magazine: body + lip
                shapes.rect(cx - s * 0.35f, cy - s, s * 0.7f, s * 1.5f)
                shapes.rect(cx - s * 0.55f, cy + s * 0.55f, s * 1.1f, s * 0.35f)
            }
            b == TouchButton.WALL -> { // a block: square ring
                shapes.rect(cx - s, cy - s, s * 2f, s * 2f)
                shapes.color = cDisc
                shapes.rect(cx - s * 0.55f, cy - s * 0.55f, s * 1.1f, s * 1.1f)
            }
            b == TouchButton.WEAPON -> { // the swap: two counter-running arrows
                shapes.rect(cx - s, cy + s * 0.15f, s * 1.4f, s * 0.25f)
                shapes.triangle(cx + s * 0.35f, cy + s * 0.75f, cx + s * 0.35f, cy - s * 0.2f, cx + s, cy + s * 0.27f)
                shapes.rect(cx - s * 0.4f, cy - s * 0.4f, s * 1.4f, s * 0.25f)
                shapes.triangle(cx - s * 0.35f, cy + s * 0.05f, cx - s * 0.35f, cy - s * 0.9f, cx - s, cy - s * 0.28f)
            }
            b == TouchButton.LAND -> { // landing: wedge down onto the pad line; takeoff: wedge up off it
                shapes.rect(cx - s, cy - s * 0.9f, s * 2f, s * 0.25f)
                if (landLabel.contains("着")) {
                    shapes.triangle(cx - s * 0.8f, cy + s * 0.9f, cx + s * 0.8f, cy + s * 0.9f, cx, cy - s * 0.3f)
                } else {
                    shapes.triangle(cx - s * 0.8f, cy - s * 0.3f, cx + s * 0.8f, cy - s * 0.3f, cx, cy + s * 0.9f)
                }
            }
            b == TouchButton.INV -> { // the satchel: box + clasp
                shapes.rect(cx - s * 0.8f, cy - s * 0.9f, s * 1.6f, s * 1.2f)
                shapes.rect(cx - s * 0.45f, cy + s * 0.4f, s * 0.9f, s * 0.3f)
            }
            b == TouchButton.FULL -> { // full throttle: twin wedges climbing
                shapes.triangle(cx - s * 0.8f, cy - s, cx + s * 0.8f, cy - s, cx, cy - s * 0.1f)
                shapes.triangle(cx - s * 0.8f, cy - s * 0.1f, cx + s * 0.8f, cy - s * 0.1f, cx, cy + s)
            }
            b == TouchButton.TUNE -> { // sliders: three rails, three heads
                for (i in 0..2) {
                    val ly = cy + s * (0.7f - 0.7f * i)
                    shapes.rect(cx - s, ly - s * 0.08f, s * 2f, s * 0.16f)
                    shapes.rect(cx - s * 0.6f + s * 0.6f * i, ly - s * 0.28f, s * 0.3f, s * 0.56f)
                }
            }
        }
    }

    /** The stick knob, clamped to the stick radius and drawn at 42% size. */
    private fun knob(shapes: ShapeRenderer, cx: Float, cy: Float, dx: Float, dy: Float, radius: Float) {
        var kx = dx; var ky = dy
        val len = hypot(kx, ky)
        if (len > radius) { kx = kx / len * radius; ky = ky / len * radius }
        shapes.circle(cx + kx, cy + ky, radius * 0.42f, 18)
    }

}
