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
    private val glyph = GlyphLayout()
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
        }
        shapes.end()
        batch.projectionMatrix = vp.camera.combined
        batch.begin()
        for (b in l.all()) {
            if (!showAll && b !in touch.visibleButtons) continue
            val label = labelOf(b, landLabel, swapMeleeFire)
            val r = l.radiusOf(b)
            // v2.54 auto-fit: a label never spills past its disc — it shrinks to fit instead.
            val baseX = font.data.scaleX; val baseY = font.data.scaleY
            glyph.setText(font, label)
            val fit = min(1f, (r * 1.7f) / glyph.width.coerceAtLeast(1f))
            if (fit < 1f) { font.data.setScale(baseX * fit, baseY * fit); glyph.setText(font, label) }
            font.color = cLabel
            font.draw(batch, glyph, l.centerX(b) - glyph.width / 2f, l.centerY(b) + glyph.height / 2f)
            font.color = Color.WHITE
            if (fit < 1f) font.data.setScale(baseX, baseY)
        }
        batch.end()
    }

    /** The stick knob, clamped to the stick radius and drawn at 42% size. */
    private fun knob(shapes: ShapeRenderer, cx: Float, cy: Float, dx: Float, dy: Float, radius: Float) {
        var kx = dx; var ky = dy
        val len = hypot(kx, ky)
        if (len > radius) { kx = kx / len * radius; ky = ky / len * radius }
        shapes.circle(cx + kx, cy + ky, radius * 0.42f, 18)
    }

    private fun labelOf(b: TouchButton, landLabel: String, swapMeleeFire: Boolean): String = when (b) {
        TouchButton.FIRE -> "射撃"
        TouchButton.MELEE -> if (swapMeleeFire) "射撃" else "近接" // v2.39: the swap puts the gun on the button
        TouchButton.DASH -> "ダッシュ"
        TouchButton.RELOAD -> "装填"
        TouchButton.WALL -> "壁"
        TouchButton.WEAPON -> "武器"
        TouchButton.LAND -> landLabel
        TouchButton.INV -> "持物"
        TouchButton.FULL -> "全開"
        TouchButton.TUNE -> "調整" // v2.98
    }
}
