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

/**
 * On-screen touch controls (Android): the move stick, the floating aim+fire stick, and the
 * contextually-visible action buttons (P3). Pure painting over input.TouchControls state —
 * GameScreen decides when to draw and what the LAND button says (着陸 vs 発進).
 */
object TouchOverlay {
    private val glyph = GlyphLayout()
    private val cStickBase = Color(1f, 1f, 1f, 0.06f)
    private val cStickKnob = Color(1f, 1f, 1f, 0.30f)
    private val cAimBase = Color(1f, 0.5f, 0.4f, 0.16f)
    private val cAimKnob = Color(1f, 0.5f, 0.4f, 0.5f)
    private val cAimGuide = Color(1f, 0.5f, 0.4f, 0.06f)
    private val cBtn = Color(1f, 1f, 1f, 0.14f)
    private val cBtnPressed = Color(1f, 1f, 1f, 0.34f)

    fun draw(
        shapes: ShapeRenderer, batch: SpriteBatch, font: BitmapFont, vp: Viewport,
        touch: TouchControls, landLabel: String,
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
        // action buttons — only the contextually-visible ones; pressed ones brighten + grow (P3).
        for (b in l.all()) {
            if (b !in touch.visibleButtons) continue
            val pressed = b in touch.pressedButtons
            shapes.color = if (pressed) cBtnPressed else cBtn
            shapes.circle(l.centerX(b), l.centerY(b), l.radiusOf(b) * (if (pressed) 1.16f else 1f), 22)
        }
        shapes.end()
        batch.projectionMatrix = vp.camera.combined
        batch.begin()
        for (b in l.all()) {
            if (b !in touch.visibleButtons) continue
            glyph.setText(font, labelOf(b, landLabel)); font.draw(batch, glyph, l.centerX(b) - glyph.width / 2f, l.centerY(b) + 7f)
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

    private fun labelOf(b: TouchButton, landLabel: String): String = when (b) {
        TouchButton.FIRE -> "射撃"
        TouchButton.MELEE -> "近接"
        TouchButton.DASH -> "ダッシュ"
        TouchButton.RELOAD -> "装填"
        TouchButton.WALL -> "壁"
        TouchButton.WEAPON -> "武器"
        TouchButton.LAND -> landLabel
    }
}
