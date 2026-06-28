package io.github.panda17tk.arpg.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.Viewport
import io.github.panda17tk.arpg.ui.UiButton

/**
 * Screen-space overlay drawing — the in-play ⏸ button, the upgrade intermission, game over,
 * pause, and help surfaces. Geometry arrives as ui.UiButton rects from ui.Modals; this object
 * only paints them, so GameScreen keeps state + wiring and stays lean (spec §5.1). All
 * coordinates are HUD dp space (y-up). Each call self-manages its ShapeRenderer/SpriteBatch
 * begin/end and projection, so it is safe to call once the per-frame HUD batch has ended.
 */
object Hud {
    private val glyph = GlyphLayout()
    private val cScrim = Color(0f, 0f, 0f, 0.6f)
    private val cScrimDark = Color(0f, 0f, 0f, 0.74f)
    private val cCard = Color(0.16f, 0.17f, 0.22f, 1f)
    private val cBtn = Color(0.18f, 0.20f, 0.27f, 1f)
    private val cBtnGo = Color(0.16f, 0.42f, 0.26f, 1f)   // green restart / resume
    private val cPill = Color(1f, 1f, 1f, 0.16f)
    private val cFrame = Color(1f, 1f, 1f, 0.45f)
    private val cHint = Color(0.75f, 0.78f, 0.85f, 1f)

    /** Small translucent ⏸ pill in the corner, drawn during play (bars drawn as shapes — no glyph). */
    fun pauseButton(shapes: ShapeRenderer, vp: Viewport, b: UiButton) {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.projectionMatrix = vp.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = cPill
        shapes.rect(b.x, b.y, b.w, b.h)
        shapes.color = Color.WHITE
        val barW = b.w * 0.13f; val barH = b.h * 0.40f
        val by = b.centerY - barH / 2f
        shapes.rect(b.centerX - barW * 1.7f, by, barW, barH)
        shapes.rect(b.centerX + barW * 0.7f, by, barW, barH)
        shapes.end()
    }

    /** Upgrade intermission: scrim, cards, header, and per-card title + description. */
    fun upgradeCards(
        shapes: ShapeRenderer, batch: SpriteBatch, font: BitmapFont, vp: Viewport,
        waveNum: Int, cards: List<UiButton>, titles: List<String>, descs: List<String>,
    ) {
        if (cards.isEmpty()) return
        val w = vp.worldWidth; val h = vp.worldHeight
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.projectionMatrix = vp.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = cScrim; shapes.rect(0f, 0f, w, h)
        cards.forEach { shapes.color = cCard; shapes.rect(it.x, it.y, it.w, it.h) }
        shapes.end()
        frames(shapes, cards)

        batch.projectionMatrix = vp.camera.combined
        batch.begin()
        val top = cards.first()
        centerText(batch, font, "ウェーブ $waveNum クリア！  強化を選択", w, top.y + top.h + 40f)
        cards.forEachIndexed { i, c ->
            font.draw(batch, "${i + 1})  ${titles.getOrElse(i) { "" }}", c.x + 16f, c.y + c.h - 16f)
            font.draw(batch, descs.getOrElse(i) { "" }, c.x + 16f, c.y + 30f)
        }
        batch.end()
    }

    /** Game over: scrim, result text, and a green 再挑戦 button with a "タップ / R" hint. */
    fun gameOver(
        shapes: ShapeRenderer, batch: SpriteBatch, font: BitmapFont, titleFont: BitmapFont, vp: Viewport,
        waveNum: Int, kills: Int, bestText: String, restart: UiButton,
    ) {
        val w = vp.worldWidth; val h = vp.worldHeight
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.projectionMatrix = vp.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = cScrimDark; shapes.rect(0f, 0f, w, h)
        shapes.color = cBtnGo; shapes.rect(restart.x, restart.y, restart.w, restart.h)
        shapes.end()
        frames(shapes, listOf(restart))

        batch.projectionMatrix = vp.camera.combined
        batch.begin()
        centerText(batch, titleFont, "ゲームオーバー", w, h / 2f + 96f)
        centerText(batch, font, "ウェーブ $waveNum    撃破 $kills", w, h / 2f + 48f)
        centerText(batch, font, bestText, w, h / 2f + 16f)
        centerLabel(batch, font, restart.label, restart.centerX, restart.centerY)
        font.color = cHint
        centerText(batch, font, "タップ / R", w, restart.y - 12f)
        font.color = Color.WHITE
        batch.end()
    }

    /** Pause overlay: scrim, title, and the stacked buttons (再開 / 最初からやり直す / 操作説明). */
    fun pause(
        shapes: ShapeRenderer, batch: SpriteBatch, font: BitmapFont, titleFont: BitmapFont, vp: Viewport,
        buttons: List<UiButton>,
    ) {
        val w = vp.worldWidth; val h = vp.worldHeight
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.projectionMatrix = vp.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = cScrimDark; shapes.rect(0f, 0f, w, h)
        buttons.forEachIndexed { i, b ->
            shapes.color = if (i == 0) cBtnGo else cBtn
            shapes.rect(b.x, b.y, b.w, b.h)
        }
        shapes.end()
        frames(shapes, buttons)

        batch.projectionMatrix = vp.camera.combined
        batch.begin()
        buttons.firstOrNull()?.let { centerText(batch, titleFont, "ポーズ", w, it.y + it.h + 56f) }
        buttons.forEach { centerLabel(batch, font, it.label, it.centerX, it.centerY) }
        batch.end()
    }

    /** Help overlay: scrim, title, static control lines, and a 戻る button. */
    fun help(
        shapes: ShapeRenderer, batch: SpriteBatch, font: BitmapFont, titleFont: BitmapFont, vp: Viewport,
        back: UiButton, lines: List<String>,
    ) {
        val w = vp.worldWidth; val h = vp.worldHeight
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.projectionMatrix = vp.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = cScrimDark; shapes.rect(0f, 0f, w, h)
        shapes.color = cBtn; shapes.rect(back.x, back.y, back.w, back.h)
        shapes.end()
        frames(shapes, listOf(back))

        batch.projectionMatrix = vp.camera.combined
        batch.begin()
        centerText(batch, titleFont, "操作説明", w, h * 0.86f)
        val left = w * 0.16f
        var y = h * 0.72f
        for (line in lines) { font.draw(batch, line, left, y); y -= 40f }
        centerLabel(batch, font, back.label, back.centerX, back.centerY)
        batch.end()
    }

    private fun frames(shapes: ShapeRenderer, rects: List<UiButton>) {
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = cFrame
        rects.forEach { shapes.rect(it.x, it.y, it.w, it.h) }
        shapes.end()
    }

    private fun centerText(batch: SpriteBatch, font: BitmapFont, s: String, screenW: Float, y: Float) {
        glyph.setText(font, s)
        font.draw(batch, glyph, (screenW - glyph.width) / 2f, y)
    }

    private fun centerLabel(batch: SpriteBatch, font: BitmapFont, s: String, cx: Float, cy: Float) {
        glyph.setText(font, s)
        font.draw(batch, glyph, cx - glyph.width / 2f, cy + glyph.height / 2f)
    }
}
