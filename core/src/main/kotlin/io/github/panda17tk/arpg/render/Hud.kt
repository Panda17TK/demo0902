package io.github.panda17tk.arpg.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.Viewport
import io.github.panda17tk.arpg.sim.DesyncGauge
import io.github.panda17tk.arpg.sim.EventKind
import io.github.panda17tk.arpg.sim.Mark
import io.github.panda17tk.arpg.sim.PlanetCardInfo
import io.github.panda17tk.arpg.sim.PlanetEvent
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.sim.VisitedMap
import io.github.panda17tk.arpg.ui.HudLayout
import io.github.panda17tk.arpg.ui.InvTab
import io.github.panda17tk.arpg.ui.InventoryLayout
import io.github.panda17tk.arpg.ui.UiButton
import io.github.panda17tk.arpg.ui.filledSegments
import kotlin.math.min

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
    private val cGlass = Color(0.04f, 0.06f, 0.10f, 0.74f)     // v2.54: hint-panel glass
    private val cGlassEdge = Color(0.55f, 0.75f, 1f, 0.22f)    // v2.54: its faint edge
    private val cTopScrim = Color(0.02f, 0.03f, 0.05f, 0.42f)  // v2.54: top-band readability scrim
    private val cScrimDark = Color(0f, 0f, 0f, 0.74f)
    private val cCard = Color(0.16f, 0.17f, 0.22f, 1f)
    private val cBtn = Color(0.18f, 0.20f, 0.27f, 1f)
    private val cBtnGo = Color(0.16f, 0.42f, 0.26f, 1f)   // green restart / resume
    private val cBtnDanger = Color(0.44f, 0.16f, 0.16f, 1f) // the destructive 消す button
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

    /** Full-screen black scrim for the landing/takeoff fade (LP 10b). Draw LAST so it covers everything. */
    fun fade(shapes: ShapeRenderer, vp: Viewport, alpha: Float) {
        if (alpha <= 0f) return
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.projectionMatrix = vp.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0f, 0f, 0f, alpha.coerceIn(0f, 1f))
        shapes.rect(0f, 0f, vp.worldWidth, vp.worldHeight)
        shapes.end()
    }

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
        centerText(batch, font, "同期汚染 $waveNum を収束 — 保守パッチを選択", w, top.y + top.h + 40f)
        cards.forEachIndexed { i, c ->
            fitText(batch, font, "${i + 1})  ${titles.getOrElse(i) { "" }}", c.x + 16f, c.y + c.h - 16f, c.w - 32f)
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
        centerText(batch, font, "同期汚染 $waveNum    撃破 $kills", w, h / 2f + 48f)
        centerText(batch, font, bestText, w, h / 2f + 16f)
        fitCenterLabel(batch, font, restart.label, restart.centerX, restart.centerY, restart.w - 12f)
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
        buttons.forEach { fitCenterLabel(batch, font, it.label, it.centerX, it.centerY, it.w - 12f) } // v2.58
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
        val left = w * 0.12f
        var y = h * 0.72f
        for (line in lines) { fitText(batch, font, line, left, y, w - left * 2f); y -= 40f } // v2.58
        fitCenterLabel(batch, font, back.label, back.centerX, back.centerY, back.w - 12f)
        batch.end()
    }

    /**
     * P2 live HUD distributed across the top band: wave badge (center), HP/stamina icon+segment
     * stack (left), weapon/ammo panel (right). Icons are ShapeRenderer figures (glyph-independent),
     * and the segmented bars carry the reading by shape+count+number, not colour alone.
     */
    /**
     * v2.54 hint panel: a centered glass card of hint lines whose text AUTO-SHRINKS to fit the
     * screen width — nothing ever spills off the edges again. [topY] is the panel's top edge
     * (callers place it just below the top HUD band, or under the scan card).
     */
    fun hintPanel(shapes: ShapeRenderer, batch: SpriteBatch, font: BitmapFont, vp: Viewport, lines: List<String>, topY: Float) {
        if (lines.isEmpty()) return
        val w = vp.worldWidth
        val maxW = w - 32f
        val baseX = font.data.scaleX; val baseY = font.data.scaleY
        var widest = 1f
        for (line in lines) { glyph.setText(font, line); if (glyph.width > widest) widest = glyph.width }
        val fit = min(1f, maxW / widest)
        val lineH = 25f * fit
        val panelW = widest * fit + 30f
        val panelH = lines.size * lineH + 16f
        val x = (w - panelW) / 2f
        val y = topY - panelH
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.projectionMatrix = vp.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = cGlassEdge; shapes.rect(x - 1.5f, y - 1.5f, panelW + 3f, panelH + 3f)
        shapes.color = cGlass; shapes.rect(x, y, panelW, panelH)
        shapes.end()
        batch.projectionMatrix = vp.camera.combined
        batch.begin()
        if (fit < 1f) font.data.setScale(baseX * fit, baseY * fit)
        lines.forEachIndexed { i, line ->
            glyph.setText(font, line)
            font.draw(batch, glyph, (w - glyph.width) / 2f, y + panelH - 10f - i * lineH)
        }
        if (fit < 1f) font.data.setScale(baseX, baseY)
        batch.end()
    }

    /** v2.56: a plain row of tappable buttons (the layout editor's toolbar). */
    fun buttonRow(shapes: ShapeRenderer, batch: SpriteBatch, font: BitmapFont, vp: Viewport, buttons: List<UiButton>) {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.projectionMatrix = vp.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        buttons.forEach { b -> shapes.color = cCard; shapes.rect(b.x, b.y, b.w, b.h) }
        shapes.end()
        frames(shapes, buttons)
        batch.projectionMatrix = vp.camera.combined
        batch.begin()
        buttons.forEach { b -> centerLabel(batch, font, b.label, b.centerX, b.centerY) }
        batch.end()
    }

    /** v2.55: draw [text] with its left edge at (x, baselineY), auto-shrunk to fit [maxW]. */
    private fun fitText(batch: SpriteBatch, font: BitmapFont, text: String, x: Float, baselineY: Float, maxW: Float) {
        val bx = font.data.scaleX; val by = font.data.scaleY
        glyph.setText(font, text)
        val fit = min(1f, maxW / glyph.width.coerceAtLeast(1f))
        if (fit < 1f) { font.data.setScale(bx * fit, by * fit); glyph.setText(font, text) }
        font.draw(batch, glyph, x, baselineY)
        if (fit < 1f) font.data.setScale(bx, by)
    }

    fun liveHud(
        shapes: ShapeRenderer, batch: SpriteBatch, font: BitmapFont, titleFont: BitmapFont, vp: Viewport,
        waveNum: Int, foes: Int,
        hp: Float, hpMax: Float, sta: Float, staMax: Float, overheat: Boolean,
        weaponName: String, mag: Int, magSize: Int?, reloadFrac: Float, reserveStr: String,
        timeSec: Float, kills: Int, blocks: Int, dust: Int = 0,
        simMode: Boolean = false, // v2.53: the old-style combat simulation shows the old badge
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
        // v2.54: one soft scrim behind the whole top band — text stays readable over bright worlds.
        shapes.color = cTopScrim; shapes.rect(0f, h - 130f, w, 130f)
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
        // v2.55: the whole sector status lives INSIDE the slim top strip — one auto-fitted line,
        // nothing dangling below it to collide with the bars (the old badge did exactly that).
        val strip = if (simMode) "ウェーブ(旧式) $waveNum　残り $foes"
            else "同期汚染 $waveNum　残プロセス $foes　宙域安定 ${DesyncGauge.stability(waveNum)}%"
        fitText(batch, font, strip, l.wave.x + 8f, l.wave.y + l.wave.h - 6f, l.wave.w - 16f)
        // HP / stamina numbers overlaid right-aligned WITHIN their bars (kept inside the left zone)
        glyph.setText(font, "${hp.toInt()}/${hpMax.toInt()}")
        font.draw(batch, glyph, l.hp.x + l.hp.w - glyph.width - 3f, l.hp.y + l.hp.h - 1f)
        if (overheat) {
            glyph.setText(font, "過熱!")
            font.draw(batch, glyph, l.stamina.x + l.stamina.w - glyph.width - 3f, l.stamina.y + l.stamina.h - 1f)
        }
        // weapon panel (right column): mag right-aligned, the name auto-fits the space that's left.
        val magStr = magSize?.let { "$mag/$it" } ?: "INF"
        glyph.setText(font, magStr)
        val magW = glyph.width
        font.draw(batch, glyph, l.ammo.x + l.ammo.w - magW - 6f, l.ammo.y + l.ammo.h - 4f)
        fitText(batch, font, weaponName, l.ammo.x + 28f, l.ammo.y + l.ammo.h - 4f, l.ammo.w - 28f - magW - 12f)
        glyph.setText(font, if (reloadFrac > 0f) "装填中" else "予備 $reserveStr")
        font.draw(batch, glyph, l.ammo.x + l.ammo.w - glyph.width - 6f, l.ammo.y + 14f)
        // secondary stats — full width, auto-fitted (v2.55: no more spill on narrow phones)
        val mins = (timeSec / 60f).toInt(); val secs = (timeSec % 60f).toInt()
        fitText(batch, font, "時間 %d:%02d  撃破 %d  資材 %d  星屑 %d".format(mins, secs, kills, blocks, dust), l.stats.x, l.stats.y + l.stats.h, l.stats.w)
        batch.end()
    }

    /**
     * Pre-landing planet scan card (LP v2.23): what the star is + whether it remembers you.
     * Geometry from HudLayout.planetCard; strings arrive pre-composed (sim/PlanetScan) so this
     * only paints. Self-manages its shape/batch passes like every other overlay here.
     */
    fun planetScanCard(
        shapes: ShapeRenderer, batch: SpriteBatch, font: BitmapFont, titleFont: BitmapFont, vp: Viewport,
        info: PlanetCardInfo, hint: String,
    ) {
        val lines = info.lines
        val card = HudLayout.planetCard(vp.worldWidth, vp.worldHeight, lines.size)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.projectionMatrix = vp.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = cGlassEdge; shapes.rect(card.x - 1.5f, card.y - 1.5f, card.w + 3f, card.h + 3f) // v2.58
        shapes.color = cHudPanel; shapes.rect(card.x, card.y, card.w, card.h)
        shapes.end()
        frames(shapes, listOf(card))

        batch.projectionMatrix = vp.camera.combined
        batch.begin()
        var y = card.y + card.h - HudLayout.CARD_PAD
        glyph.setText(titleFont, info.title)
        titleFont.draw(batch, glyph, card.centerX - glyph.width / 2f, y)
        y -= HudLayout.CARD_TITLE_H
        for (line in lines) {
            fitText(batch, font, line, card.x + 14f, y, card.w - 28f) // v2.58
            y -= HudLayout.CARD_LINE_H
        }
        font.color = cHint
        glyph.setText(font, hint)
        font.draw(batch, glyph, card.centerX - glyph.width / 2f, card.y + HudLayout.CARD_PAD + HudLayout.CARD_HINT_H - 4f)
        font.color = Color.WHITE
        batch.end()
    }

    /**
     * Planet-memory summary (LP v2.25): the surface pause's read-only 「この星の記憶」 screen.
     * Header (traits) → fact lines with a ○/−/× mark → the three feeling gauges → a 戻る button.
     * Strings and values arrive pre-composed (sim/SocietyMemorySummary + PlanetLexicon); this only paints.
     */
    fun memory(
        shapes: ShapeRenderer, batch: SpriteBatch, font: BitmapFont, titleFont: BitmapFont, vp: Viewport,
        header: String, facts: List<Pair<String, Mark>>, gauges: List<Pair<String, Float>>, back: UiButton,
        goals: List<String> = emptyList(), // LP v2.26: the planet's full goal list (already chip-formatted)
    ) {
        val w = vp.worldWidth; val h = vp.worldHeight
        // Gauge bars sit between the back button and the fact list; labels to their left.
        val barW = min(220f, w * 0.45f)
        val barX = (w - barW) / 2f + 36f
        val gaugeBase = back.y + back.h + 24f
        val rowH = 26f

        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.projectionMatrix = vp.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = cScrimDark; shapes.rect(0f, 0f, w, h)
        shapes.color = cBtn; shapes.rect(back.x, back.y, back.w, back.h)
        gauges.reversed().forEachIndexed { i, (_, value) ->
            segBar(shapes, UiButton(barX, gaugeBase + i * rowH, barW, 12f), value, 1f, 8, cStaFill)
        }
        shapes.end()
        frames(shapes, listOf(back))

        batch.projectionMatrix = vp.camera.combined
        batch.begin()
        centerText(batch, titleFont, "この星の記憶", w, h * 0.9f)
        centerText(batch, font, header, w, h * 0.82f)
        var y = h * 0.74f
        val left = w * 0.18f
        for ((text, mark) in facts) {
            val markGlyph = when (mark) {
                Mark.DONE -> "○"
                Mark.NONE -> "−"
                Mark.BAD -> "×"
            }
            fitText(batch, font, "$markGlyph  $text", left, y, w - left * 2f)
            y -= rowH
        }
        if (goals.isNotEmpty()) { // the planet's goals, below the facts (chip strings carry their own marks)
            y -= 6f
            font.color = cHint
            for (g in goals) { font.draw(batch, g, left, y); y -= rowH }
            font.color = Color.WHITE
        }
        gauges.reversed().forEachIndexed { i, (label, _) ->
            font.draw(batch, label, barX - 70f, gaugeBase + i * rowH + 13f)
        }
        fitCenterLabel(batch, font, back.label, back.centerX, back.centerY, back.w - 12f)
        batch.end()
    }

    /**
     * 「宇宙の記憶を消す」 confirmation (LP v2.28): a dark scrim, the warning, and [消す][戻る].
     * The destructive button wears a warning red; nothing here mutates state — GameScreen acts on the tap.
     */
    fun forget(
        shapes: ShapeRenderer, batch: SpriteBatch, font: BitmapFont, titleFont: BitmapFont, vp: Viewport,
        buttons: List<UiButton>,
    ) {
        val w = vp.worldWidth; val h = vp.worldHeight
        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.projectionMatrix = vp.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = cScrimDark; shapes.rect(0f, 0f, w, h)
        buttons.forEachIndexed { i, b ->
            shapes.color = if (i == 0) cBtnDanger else cBtn
            shapes.rect(b.x, b.y, b.w, b.h)
        }
        shapes.end()
        frames(shapes, buttons)

        batch.projectionMatrix = vp.camera.combined
        batch.begin()
        buttons.firstOrNull()?.let {
            centerText(batch, titleFont, "宇宙の記憶を消す", w, it.y + it.h + 96f)
            centerText(batch, font, "本当に消しますか", w, it.y + it.h + 56f)
            centerText(batch, font, "この宇宙のすべての星があなたを忘れます", w, it.y + it.h + 30f)
        }
        buttons.forEach { fitCenterLabel(batch, font, it.label, it.centerX, it.centerY, it.w - 12f) }
        batch.end()
    }

    /**
     * Surface event feed (LP v2.24): up to a few short lines top-left, under the stats row.
     * Oldest at the top, newest stacking downward; each line fades over its last moments.
     * Colours by kind: hostile red / mercy green / ecology amber / neutral ink.
     */
    fun eventFeed(batch: SpriteBatch, font: BitmapFont, vp: Viewport, events: List<PlanetEvent>) {
        if (events.isEmpty()) return
        val l = HudLayout.of(vp.worldWidth, vp.worldHeight)
        batch.projectionMatrix = vp.camera.combined
        batch.begin()
        var y = l.stats.y - 8f
        for (e in events) {
            val alpha = ((Tuning.EVENT_FEED_LIFE - e.age) / Tuning.EVENT_FEED_FADE).coerceIn(0f, 1f)
            val base = when (e.kind) {
                EventKind.HOSTILE -> cHpLo
                EventKind.MERCY -> cHpHi
                EventKind.ECOLOGY -> cReload
                EventKind.NEUTRAL -> cHudInk
            }
            font.setColor(base.r, base.g, base.b, alpha)
            font.draw(batch, e.text, l.stats.x, y)
            y -= 20f
        }
        font.color = Color.WHITE
        batch.end()
    }

    /**
     * インベントリ (v2.33): a light scrim (the world crawls at 0.01× behind it), the tab strip,
     * one tab's body, and the close button. Pure painting — GameScreen owns the tab state and
     * acts on taps against the same InventoryLayout rects.
     */
    @Suppress("LongParameterList")
    fun inventory(
        shapes: ShapeRenderer, batch: SpriteBatch, font: BitmapFont, titleFont: BitmapFont, vp: Viewport,
        tab: InvTab,
        slotTexts: List<String>,   // EQUIP rows, aligned with InventoryLayout.slotRows
        itemLines: List<String>,   // ITEMS tab rows (already formatted, aligned with itemRows)
        visited: VisitedMap?, playerTx: Int, playerTy: Int, // MAP tab
        note: String?,             // brief flash on ITEMS/SAVE (「セーブした」, a consumable's effect…)
        loreTitle: String? = null, loreLines: List<String> = emptyList(), // v2.34: the open readable
        controlLabel: String? = null, // v2.39: EQUIP tab's control-swap toggle caption
        marketLines: List<String> = emptyList(), // v2.43: MARKET rows (already priced/formatted)
        marketFooter: String? = null,            // v2.43: dust balance or the closed-stall notice
        logLines: List<String> = emptyList(),    // v2.46: 記録 tab (already formatted, top-down)
        layoutEditLabel: String? = null,         // v2.56: EQUIP tab's layout-editor entry strip
    ) {
        val w = vp.worldWidth; val h = vp.worldHeight
        val panel = InventoryLayout.panel(w, h)
        val tabs = InventoryLayout.tabs(w, h)
        val body = InventoryLayout.body(w, h)
        val close = InventoryLayout.closeButton(w, h)
        val slotRows = if (tab == InvTab.EQUIP) InventoryLayout.slotRows(w, h) else emptyList()
        val save = if (tab == InvTab.SAVE) InventoryLayout.saveButton(w, h) else null
        val toggle = if (tab == InvTab.EQUIP && controlLabel != null) InventoryLayout.controlToggle(w, h) else null
        val editStrip = if (tab == InvTab.EQUIP && layoutEditLabel != null) InventoryLayout.layoutEditToggle(w, h) else null
        val marketRows = if (tab == InvTab.MARKET) InventoryLayout.marketRows(w, h, marketLines.size) else emptyList()

        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.projectionMatrix = vp.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = cScrim; shapes.rect(0f, 0f, w, h)
        // v2.57: the panel wears the same glass as the hint cards — faint edge, deep body.
        shapes.color = cGlassEdge; shapes.rect(panel.x - 1.5f, panel.y - 1.5f, panel.w + 3f, panel.h + 3f)
        shapes.color = cCard; shapes.rect(panel.x, panel.y, panel.w, panel.h)
        tabs.forEachIndexed { i, t ->
            shapes.color = if (i == tab.ordinal) cBtnGo else cBtn
            shapes.rect(t.x, t.y, t.w, t.h)
        }
        slotRows.forEach { r -> shapes.color = cBtn; shapes.rect(r.x, r.y, r.w, r.h) }
        save?.let { shapes.color = cBtnGo; shapes.rect(it.x, it.y, it.w, it.h) }
        toggle?.let { shapes.color = cBtn; shapes.rect(it.x, it.y, it.w, it.h) }
        editStrip?.let { shapes.color = cBtn; shapes.rect(it.x, it.y, it.w, it.h) }
        marketRows.forEach { r -> shapes.color = cBtn; shapes.rect(r.x, r.y, r.w, r.h) }
        shapes.color = cBtn; shapes.rect(close.x, close.y, close.w, close.h)
        if (tab == InvTab.MAP && visited != null) drawVisitedMap(shapes, body, visited, playerTx, playerTy)
        shapes.end()
        frames(shapes, tabs + slotRows + marketRows + listOfNotNull(save, toggle, editStrip) + listOf(close))

        batch.projectionMatrix = vp.camera.combined
        batch.begin()
        tabs.forEach { fitCenterLabel(batch, font, it.label, it.centerX, it.centerY, it.w - 8f) } // v2.57
        when (tab) {
            InvTab.EQUIP -> {
                slotRows.forEachIndexed { i, r ->
                    fitText(batch, font, slotTexts.getOrElse(i) { "" }, r.x + 12f, r.centerY + 7f, r.w - 24f)
                }
                // v2.39: the bottom strip is the control-swap toggle (falls back to the old hint).
                if (editStrip != null && layoutEditLabel != null) {
                    fitCenterLabel(batch, font, layoutEditLabel, editStrip.centerX, editStrip.centerY, editStrip.w - 16f)
                }
                if (toggle != null && controlLabel != null) {
                    fitCenterLabel(batch, font, controlLabel, toggle.centerX, toggle.centerY, toggle.w - 16f)
                } else {
                    font.color = cHint
                    fitCenterLabel(batch, font, "スロットをタップで持物と交換", w / 2f, body.y + 22f, body.w - 8f)
                    font.color = Color.WHITE
                }
            }
            InvTab.ITEMS -> {
                if (loreTitle != null) {
                    // Reading view (v2.34): the open readable's title + its text, tap anywhere to return.
                    fitCenterLabel(batch, titleFont, loreTitle, w / 2f, body.y + body.h - 20f, body.w - 8f)
                    var y = body.y + body.h - 52f
                    for (line in loreLines) {
                        fitText(batch, font, line, body.x + 12f, y, body.w - 24f)
                        y -= 24f
                        if (y < body.y + 34f) break
                    }
                    font.color = cHint
                    centerText(batch, font, "タップで戻る", w, body.y + 16f)
                    font.color = Color.WHITE
                } else {
                    val rows = InventoryLayout.itemRows(w, h, itemLines.size)
                    rows.forEachIndexed { i, r -> fitText(batch, font, itemLines[i], r.x + 12f, r.y + 18f, r.w - 24f) }
                    if (itemLines.isEmpty()) {
                        font.color = cHint; font.draw(batch, "持物は空", body.x + 12f, body.y + body.h - 10f); font.color = Color.WHITE
                    }
                    font.color = cHint
                    fitCenterLabel(batch, font, note ?: "タップ：消費アイテムを使う / 読み物を読む", w / 2f, body.y + 20f, body.w - 8f)
                    font.color = Color.WHITE
                }
            }
            InvTab.MAP -> {
                font.color = cHint
                centerText(batch, font, "通った場所だけが描かれる", w, body.y + 16f)
                font.color = Color.WHITE
            }
            InvTab.MARKET -> { // v2.43: the planet's stalls (or the reason they're shut)
                marketRows.forEachIndexed { i, r -> fitText(batch, font, marketLines[i], r.x + 12f, r.centerY + 7f, r.w - 24f) }
                font.color = cHint
                marketFooter?.let { fitCenterLabel(batch, font, it, w / 2f, body.y + 20f, body.w - 8f) }
                font.color = Color.WHITE
            }
            InvTab.SAVE -> {
                save?.let { centerLabel(batch, titleFont, it.label, it.centerX, it.centerY) }
                font.color = cHint
                fitCenterLabel(batch, font, "この場でランを保存する（やられると消える）", w / 2f, body.y + body.h * 0.30f, body.w - 8f)
                font.color = Color.WHITE
                note?.let { fitCenterLabel(batch, font, it, w / 2f, body.y + body.h * 0.22f, body.w - 8f) }
            }
            InvTab.LOG -> { // v2.46 航海日誌: the run so far + what the stars remember
                var ly = body.y + body.h - 14f
                for (line in logLines) {
                    fitText(batch, font, line, body.x + 12f, ly, body.w - 24f)
                    ly -= 24f
                    if (ly < body.y + 34f) break
                }
                font.color = cHint
                centerText(batch, font, "旅の記録 — 星々はあなたを覚えている", w, body.y + 16f)
                font.color = Color.WHITE
            }
        }
        fitCenterLabel(batch, font, close.label, close.centerX, close.centerY, close.w - 12f)
        batch.end()
    }

    /** MAP tab: visited tiles as tiny rects fitted to the body; the player as a bright dot. */
    private fun drawVisitedMap(shapes: ShapeRenderer, body: UiButton, visited: VisitedMap, playerTx: Int, playerTy: Int) {
        if (visited.w <= 0 || visited.h <= 0) return
        val scale = min(body.w / visited.w, body.h / visited.h)
        val ox = body.x + (body.w - visited.w * scale) / 2f
        // World tiles are y-down; HUD space is y-up — flip so north on the map is up on the screen.
        val oy = body.y + (body.h - visited.h * scale) / 2f
        val cell = scale.coerceAtLeast(1f)
        shapes.color = cHudInk
        visited.forEachVisited { tx, ty ->
            shapes.rect(ox + tx * scale, oy + (visited.h - 1 - ty) * scale, cell, cell)
        }
        shapes.color = cReload
        shapes.rect(ox + playerTx * scale - cell, oy + (visited.h - 1 - playerTy) * scale - cell, cell * 3f, cell * 3f)
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

    /** v2.57: centered label that auto-shrinks to [maxW] — tabs/strips can't smear together. */
    private fun fitCenterLabel(batch: SpriteBatch, font: BitmapFont, s: String, cx: Float, cy: Float, maxW: Float) {
        val bx = font.data.scaleX; val by = font.data.scaleY
        glyph.setText(font, s)
        val fit = min(1f, maxW / glyph.width.coerceAtLeast(1f))
        if (fit < 1f) { font.data.setScale(bx * fit, by * fit); glyph.setText(font, s) }
        font.draw(batch, glyph, cx - glyph.width / 2f, cy + glyph.height / 2f)
        if (fit < 1f) font.data.setScale(bx, by)
    }
}
