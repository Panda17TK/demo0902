package io.github.panda17tk.arpg.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.Viewport
import io.github.panda17tk.arpg.ui.HudLayout
import io.github.panda17tk.arpg.ui.UiButton
import io.github.panda17tk.arpg.ui.filledSegments

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

    // P2 live-HUD palette (moved out of GameScreen).
    private val cHudPanel = Color(0.05f, 0.07f, 0.10f, 0.72f)
    private val cSegEmpty = Color(0.22f, 0.22f, 0.27f, 0.9f)
    private val cStaFill = Color.valueOf("4da6ff")
    private val cOver = Color.valueOf("ff5a3a")
    private val cHpHi = Color.valueOf("7fe08a")
    private val cHpLo = Color.valueOf("e0786a")
    private val cHudInk = Color(0.85f, 0.88f, 0.95f, 1f)
    private val cReload = Color.valueOf("ffd166")

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

    /**
     * P2 live HUD distributed across the top band: wave badge (center), HP/stamina icon+segment
     * stack (left), weapon/ammo panel (right). Icons are ShapeRenderer figures (glyph-independent),
     * and the segmented bars carry the reading by shape+count+number, not colour alone.
     */
    fun liveHud(
        shapes: ShapeRenderer, batch: SpriteBatch, font: BitmapFont, titleFont: BitmapFont, vp: Viewport,
        waveNum: Int, foes: Int,
        hp: Float, hpMax: Float, sta: Float, staMax: Float, overheat: Boolean,
        weaponName: String, mag: Int, magSize: Int?, reloadFrac: Float, reserveStr: String,
        timeSec: Float, kills: Int, blocks: Int,
    ) {
        val w = vp.worldWidth; val h = vp.worldHeight
        val l = HudLayout.of(w, h)
        val seg = 12
        val hpFrac = if (hpMax > 0f) (hp / hpMax).coerceIn(0f, 1f) else 0f
        val hpCol = if (hpFrac > 0.3f) cHpHi else cHpLo
        val staCol = if (overheat) cOver else cStaFill

        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.projectionMatrix = vp.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = cHudPanel; shapes.rect(l.wave.x, l.wave.y, l.wave.w, l.wave.h)
        shapes.color = cHudPanel; shapes.rect(l.ammo.x, l.ammo.y, l.ammo.w, l.ammo.h)
        heart(shapes, l.hp.x - 11f, l.hp.centerY, 8f, hpCol)
        segBar(shapes, l.hp, hp, hpMax, seg, hpCol)
        bolt(shapes, l.stamina.x - 11f, l.stamina.centerY, 8f, staCol)
        segBar(shapes, l.stamina, sta, staMax, seg, staCol)
        gun(shapes, l.ammo.x + 15f, l.ammo.centerY, 9f, cHudInk)
        if (reloadFrac > 0f) {
            shapes.color = cReload
            shapes.rect(l.ammo.x + 4f, l.ammo.y + 3f, (l.ammo.w - 8f) * reloadFrac.coerceIn(0f, 1f), 3f)
        }
        shapes.end()

        batch.projectionMatrix = vp.camera.combined
        batch.begin()
        // wave badge: small label + big right-aligned number, "残り N" centered just below
        font.draw(batch, "ウェーブ", l.wave.x + 8f, l.wave.y + l.wave.h - 8f)
        glyph.setText(titleFont, "$waveNum")
        titleFont.draw(batch, glyph, l.wave.x + l.wave.w - glyph.width - 10f, l.wave.y + l.wave.h - 3f)
        glyph.setText(font, "残り $foes")
        font.draw(batch, glyph, l.wave.centerX - glyph.width / 2f, l.wave.y - 2f)
        // HP / stamina numbers overlaid right-aligned WITHIN their bars (kept inside the left zone)
        glyph.setText(font, "${hp.toInt()}/${hpMax.toInt()}")
        font.draw(batch, glyph, l.hp.x + l.hp.w - glyph.width - 3f, l.hp.y + l.hp.h - 1f)
        if (overheat) {
            glyph.setText(font, "過熱!")
            font.draw(batch, glyph, l.stamina.x + l.stamina.w - glyph.width - 3f, l.stamina.y + l.stamina.h - 1f)
        }
        // weapon panel (right zone): name + big mag, reserve / reloading below — right-aligned, compact
        font.draw(batch, weaponName, l.ammo.x + 30f, l.ammo.y + l.ammo.h - 4f)
        val magStr = magSize?.let { "$mag/$it" } ?: "INF"
        glyph.setText(font, magStr)
        font.draw(batch, glyph, l.ammo.x + l.ammo.w - glyph.width - 6f, l.ammo.y + l.ammo.h - 4f)
        glyph.setText(font, if (reloadFrac > 0f) "装填中" else "予備 $reserveStr")
        font.draw(batch, glyph, l.ammo.x + l.ammo.w - glyph.width - 6f, l.ammo.y + 14f)
        // secondary stats (lowest priority)
        val mins = (timeSec / 60f).toInt(); val secs = (timeSec % 60f).toInt()
        font.draw(batch, "時間 %d:%02d  撃破 %d  資材 %d".format(mins, secs, kills, blocks), l.stats.x, l.stats.y + l.stats.h)
        batch.end()
    }

    private fun segBar(shapes: ShapeRenderer, b: UiButton, value: Float, max: Float, count: Int, fill: Color) {
        val lit = filledSegments(value, max, count)
        val gap = 2f
        val segW = (b.w - (count - 1) * gap) / count
        for (i in 0 until count) {
            shapes.color = if (i < lit) fill else cSegEmpty
            shapes.rect(b.x + i * (segW + gap), b.y, segW, b.h)
        }
    }

    private fun heart(shapes: ShapeRenderer, cx: Float, cy: Float, s: Float, color: Color) {
        shapes.color = color
        shapes.circle(cx - s * 0.32f, cy + s * 0.18f, s * 0.36f, 10)
        shapes.circle(cx + s * 0.32f, cy + s * 0.18f, s * 0.36f, 10)
        shapes.triangle(cx - s * 0.62f, cy + s * 0.24f, cx + s * 0.62f, cy + s * 0.24f, cx, cy - s * 0.66f)
    }

    private fun bolt(shapes: ShapeRenderer, cx: Float, cy: Float, s: Float, color: Color) {
        shapes.color = color
        shapes.triangle(cx - s * 0.35f, cy + s * 0.7f, cx + s * 0.25f, cy + s * 0.1f, cx - s * 0.05f, cy + s * 0.1f)
        shapes.triangle(cx + s * 0.35f, cy - s * 0.7f, cx - s * 0.25f, cy - s * 0.1f, cx + s * 0.05f, cy - s * 0.1f)
    }

    private fun gun(shapes: ShapeRenderer, cx: Float, cy: Float, s: Float, color: Color) {
        shapes.color = color
        shapes.rect(cx - s * 0.7f, cy - s * 0.12f, s * 1.4f, s * 0.4f)
        shapes.rect(cx - s * 0.5f, cy - s * 0.7f, s * 0.32f, s * 0.6f)
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
