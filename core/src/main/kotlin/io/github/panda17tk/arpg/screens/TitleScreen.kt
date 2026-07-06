package io.github.panda17tk.arpg.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport
import io.github.panda17tk.arpg.App
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.render.Fonts
import io.github.panda17tk.arpg.audio.Ambience
import io.github.panda17tk.arpg.audio.AmbientTrack
import io.github.panda17tk.arpg.audio.Sfx
import io.github.panda17tk.arpg.input.Haptics
import io.github.panda17tk.arpg.save.Achievements
import io.github.panda17tk.arpg.save.PreferencesRunSaveStore
import io.github.panda17tk.arpg.save.Scores
import io.github.panda17tk.arpg.ui.RecordsPanel
import io.github.panda17tk.arpg.save.Workshop
import io.github.panda17tk.arpg.save.WorkshopCatalog
import io.github.panda17tk.arpg.ui.SettingsPanel
import io.github.panda17tk.arpg.ui.WorkshopPanel
import io.github.panda17tk.arpg.ui.TitleFx
import io.github.panda17tk.arpg.ui.TitleLayout
import io.github.panda17tk.arpg.ui.UiButton

/**
 * v2.58 タイトル画面: the front door the game deserved — the drifting star field, the name,
 * the premise in one quiet line, and three ways in (continue / new run / the old combat sim).
 * Same glass aesthetic as the in-game overlays; no assets, all procedural.
 */
class TitleScreen(private val app: App) : ScreenAdapter() {
    private lateinit var shapes: ShapeRenderer
    private lateinit var batch: SpriteBatch
    private lateinit var viewport: ScreenViewport
    private val glyph = GlyphLayout()
    private val tmp = Vector3()
    private var t = 0f
    private var hasSave = false
    private var showRecords = false // v2.64 記録: the service-record overlay
    private var diagQueued = false  // v2.64: 起動診断をもう一度 was pressed this visit
    private var showSettings = false // v2.66 設定: the settings-panel overlay
    private var showWorkshop = false // v2.90 工房: the workshop overlay
    private var leftyOn = false     // v2.65 左利き配置 (applied by the game screen on entry)
    private var hintsOn = true      // v2.66 操作ヒント (applied by the game screen on entry)
    private var loreOn = true       // v2.66 世界観ヒント (applied by the game screen on entry)

    // A deterministic drifting star field: fraction positions + parallax speed per star.
    private data class Star(val fx: Float, val fy: Float, val size: Float, val speed: Float)
    private val stars: List<Star> = run {
        val r = Rng(7L)
        List(110) { Star(r.nextFloat(), r.nextFloat(), 0.8f + r.nextFloat() * 1.8f, 2f + r.nextFloat() * 10f) }
    }

    private val cStar = Color(0.85f, 0.9f, 1f, 0.7f)
    private val cNebulaA = Color(0.30f, 0.40f, 0.75f, 0.05f) // v2.71: far clouds, two hues
    private val cNebulaB = Color(0.55f, 0.35f, 0.70f, 0.04f)
    private val cTwinkle = Color() // v2.71: scratch colour for per-frame alpha work
    private val cPlanet = Color(0.10f, 0.14f, 0.24f, 1f)
    private val cPlanetRim = Color(0.35f, 0.55f, 0.85f, 0.25f)
    private val cGate = Color(0.35f, 0.8f, 1f, 0.35f)
    private val cSub = Color(0.62f, 0.68f, 0.80f, 1f)

    override fun show() {
        shapes = ShapeRenderer()
        batch = SpriteBatch()
        val uiScale = Gdx.graphics.density.coerceIn(1f, 4f)
        Fonts.load(uiScale)
        viewport = ScreenViewport()
        viewport.setUnitsPerPixel(1f / uiScale)
        viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
        hasSave = try { PreferencesRunSaveStore().load() != null } catch (_: Throwable) { false }
        Scores.load() // v2.62: the training scoreboard shows on the front door
        Achievements.load() // v2.64 記録: the service record reads from here
        Workshop.load() // v2.90 工房: the ledger and its ranks
        try { // v2.59 設定: restore the sound / haptics switches
            val sp = Gdx.app.getPreferences("drift-settings")
            Sfx.enabled = sp.getBoolean("soundOn", true)
            Haptics.enabled = sp.getBoolean("hapticsOn", true)
            leftyOn = sp.getBoolean("leftHanded", false) // v2.65
            hintsOn = sp.getBoolean("controlHintsOn", true) // v2.66
            loreOn = sp.getBoolean("loreHintsOn", true)     // v2.66
        } catch (_: Throwable) { /* defaults stay on */ }
        Ambience.setEnabled(Sfx.enabled) // v2.63: the サウンド toggle gates the ambient loop too
        Ambience.play(AmbientTrack.TITLE)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun render(delta: Float) {
        t += delta
        ScreenUtils.clear(0.03f, 0.04f, 0.07f, 1f)
        viewport.apply()
        val w = viewport.worldWidth
        val h = viewport.worldHeight
        val buttons = TitleLayout.buttons(w, h, hasSave)

        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapes.projectionMatrix = viewport.camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        // v2.71: nebula clouds far behind everything — huge, dim, barely drifting
        val minDim = minOf(w, h)
        for ((ci, c) in TitleFx.CLOUDS.withIndex()) {
            val r = c.fr * minDim
            val x = (c.fx * w + t * c.speed) % (w + r * 2f) - r
            shapes.color = if (ci % 2 == 0) cNebulaA else cNebulaB
            shapes.circle(x, c.fy * h, r, 40)
            shapes.circle(x + r * 0.55f, c.fy * h - r * 0.25f, r * 0.7f, 32)
        }
        // drifting stars (wrap horizontally; slow parallax by size); v2.71: each twinkles
        for ((i, s) in stars.withIndex()) {
            val x = (s.fx * w + t * s.speed) % w
            cTwinkle.set(cStar)
            cTwinkle.a = cStar.a * TitleFx.twinkle(i, t)
            shapes.color = cTwinkle
            shapes.circle(x, s.fy * h, s.size, 6)
        }
        // v2.71: an occasional meteor, falling down-left with a fading tail
        TitleFx.meteorAt(t)?.let { m ->
            val fade = 1f - m.p
            for (k in 0..3) {
                val back = k * 0.045f
                cTwinkle.set(0.85f, 0.93f, 1f, (0.65f - k * 0.16f) * fade)
                shapes.color = cTwinkle
                shapes.circle((m.fx - m.dirX * back) * w, (m.fy - m.dirY * back) * h, 2.4f - k * 0.5f, 8)
            }
        }
        // a dark planet rising from the bottom-left — the server-world, waiting
        shapes.color = cPlanetRim
        shapes.circle(w * 0.12f, -h * 0.28f, h * 0.46f, 64)
        shapes.color = cPlanet
        shapes.circle(w * 0.12f, -h * 0.28f, h * 0.45f, 64)
        // v2.71: city lights of the sleeping servers, slowly carried around the limb
        for (k in 0 until 5) {
            val a = t * 0.04f + k * 1.3f
            val lx = w * 0.12f + kotlin.math.cos(a) * h * 0.42f
            val ly = -h * 0.28f + kotlin.math.sin(a) * h * 0.42f
            if (ly > 0f && kotlin.math.sin(a) > 0f) { // only on the visible upper limb
                cTwinkle.set(0.65f, 0.8f, 1f, 0.35f + 0.15f * TitleFx.twinkle(k, t))
                shapes.color = cTwinkle
                shapes.circle(lx, ly, 1.8f, 6)
            }
        }
        // v2.71: a soft halo behind the logo, breathing with TitleFx.glow
        cTwinkle.set(0.55f, 0.75f, 1f, TitleFx.glow(t))
        shapes.color = cTwinkle
        shapes.circle(w / 2f, h * 0.72f, minDim * 0.22f, 48)
        // a far jump-gate ring, breathing
        val breath = 0.5f + 0.5f * kotlin.math.sin(t * 0.8f)
        shapes.color = cGate
        shapes.circle(w * 0.82f, h * 0.80f, 16f + 3f * breath, 24)
        shapes.color = Color(0.8f, 0.97f, 1f, 0.8f)
        shapes.circle(w * 0.82f, h * 0.80f, 3f, 10)
        // menu buttons (glass cards) + the two corner chips (設定 left, 記録 right)
        val rec = TitleLayout.recordsButton(w, h)
        val set = TitleLayout.settingsButton(w, h)
        val wsh = TitleLayout.workshopButton(w, h) // v2.90
        (buttons + rec + set + wsh).forEach { b ->
            shapes.color = Color(0.55f, 0.75f, 1f, 0.22f)
            shapes.rect(b.x - 1.5f, b.y - 1.5f, b.w + 3f, b.h + 3f)
            shapes.color = Color(0.05f, 0.07f, 0.11f, 0.85f)
            shapes.rect(b.x, b.y, b.w, b.h)
        }
        // v2.73 通知バッジ: unseen unlocks glow on the 記録 chip's shoulder
        val unseen = Achievements.unseenCount()
        if (unseen > 0) {
            shapes.color = Color(1f, 0.72f, 0.30f, 0.95f)
            shapes.circle(rec.x + rec.w - 4f, rec.y + rec.h - 2f, 9f, 16)
        }
        shapes.end()

        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        // the name, big and quiet
        val title = Fonts.title
        val tx = title.data.scaleX; val ty = title.data.scaleY
        title.data.setScale(tx * 1.5f, ty * 1.5f)
        glyph.setText(title, "drift")
        title.draw(batch, glyph, (w - glyph.width) / 2f, h * 0.74f)
        title.data.setScale(tx, ty)
        val font = Fonts.ui
        font.color = cSub
        // v2.84: one quiet line, auto-fitted — the tagline used to run off both screen edges.
        drawFitted(font, "慣性で漂う宇宙と、あなたを覚えている星々。", w / 2f, h * 0.62f, w - 48f)
        font.color = Color.WHITE
        (buttons + rec + set + wsh).forEach { b ->
            glyph.setText(font, b.label)
            font.draw(batch, glyph, b.centerX - glyph.width / 2f, b.centerY + glyph.height / 2f)
        }
        if (unseen > 0) { // v2.73: the count sits inside the badge
            font.color = Color(0.08f, 0.06f, 0.03f, 1f)
            glyph.setText(font, if (unseen > 9) "9+" else "$unseen")
            font.draw(batch, glyph, rec.x + rec.w - 4f - glyph.width / 2f, rec.y + rec.h - 2f + glyph.height / 2f)
            font.color = Color.WHITE
        }
        font.color = cSub
        if (Scores.simBestWave > 0) { // v2.62 訓練スコアボード
            drawFitted(font, "訓練記録　ウェーブ ${Scores.simBestWave}　撃破 ${Scores.simBestKills}", w / 2f, h * 0.10f, w - 48f)
        }
        font.color = Color.WHITE
        batch.end()

        if (showRecords) drawRecords(w, h) // v2.64: the service record sits over everything
        if (showSettings) drawSettings(w, h) // v2.66: so does the settings panel
        if (showWorkshop) drawWorkshop(w, h) // v2.90: and the workshop
        handleInput(buttons, rec, set, wsh)
    }

    /** v2.66 設定: dim + glass panel + the five toggles (状態つき) + 閉じる. */
    private fun drawSettings(w: Float, h: Float) {
        val btns = SettingsPanel.buttons(w, h)
        val px = 14f; val pw = w - 28f; val pTop = h * 0.88f; val pBot = h * 0.10f
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, 0.72f); shapes.rect(0f, 0f, w, h)
        shapes.color = Color(0.55f, 0.75f, 1f, 0.22f)
        shapes.rect(px - 1.5f, pBot - 1.5f, pw + 3f, (pTop - pBot) + 3f)
        shapes.color = Color(0.05f, 0.07f, 0.11f, 0.94f)
        shapes.rect(px, pBot, pw, pTop - pBot)
        btns.forEach { b ->
            shapes.color = Color(0.55f, 0.75f, 1f, 0.22f)
            shapes.rect(b.x - 1.5f, b.y - 1.5f, b.w + 3f, b.h + 3f)
            shapes.color = Color(0.09f, 0.12f, 0.18f, 0.95f)
            shapes.rect(b.x, b.y, b.w, b.h)
        }
        shapes.end()
        batch.begin()
        val font = Fonts.ui
        font.color = cSub
        // v2.84: the header owns the panel head — it used to print straight over the first row.
        drawFitted(font, "設定", w / 2f, pTop - 22f, pw - 24f)
        font.color = Color.WHITE
        btns.forEach { b ->
            val text = if (b.label == SettingsPanel.CLOSE_LABEL) b.label
            else "${b.label}: ${if (toggleState(b.label)) "ON" else "OFF"}"
            if (b.label == SettingsPanel.CLOSE_LABEL) {
                drawFitted(font, text, b.centerX, b.centerY + 9f, b.w - 24f)
            } else { // two lines inside the taller row: state up top, its whisper below, smaller
                drawFitted(font, text, b.centerX, b.centerY + 22f, b.w - 24f)
                font.color = cSub
                drawFitted(font, SettingsPanel.hintFor(b.label), b.centerX, b.centerY - 2f, b.w - 24f, scale = 0.85f)
                font.color = Color.WHITE
            }
        }
        batch.end()
    }

    /** v2.90 工房: dim + glass panel + the fragment bank + one row per craft + 閉じる. */
    private fun drawWorkshop(w: Float, h: Float) {
        val btns = WorkshopPanel.buttons(w, h)
        val px = 14f; val pw = w - 28f; val pTop = h * 0.88f; val pBot = h * 0.10f
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, 0.72f); shapes.rect(0f, 0f, w, h)
        shapes.color = Color(0.55f, 0.75f, 1f, 0.22f)
        shapes.rect(px - 1.5f, pBot - 1.5f, pw + 3f, (pTop - pBot) + 3f)
        shapes.color = Color(0.05f, 0.07f, 0.11f, 0.94f)
        shapes.rect(px, pBot, pw, pTop - pBot)
        btns.forEach { b ->
            shapes.color = Color(0.55f, 0.75f, 1f, 0.22f)
            shapes.rect(b.x - 1.5f, b.y - 1.5f, b.w + 3f, b.h + 3f)
            shapes.color = Color(0.09f, 0.12f, 0.18f, 0.95f)
            shapes.rect(b.x, b.y, b.w, b.h)
        }
        shapes.end()
        batch.begin()
        val font = Fonts.ui
        font.color = cSub
        drawFitted(font, "工房", w / 2f, pTop - 22f, pw - 24f)
        drawFitted(font, "記録断片 ${Workshop.bank}", w / 2f, pTop - 46f, pw - 24f, scale = 0.9f)
        font.color = Color.WHITE
        btns.forEach { b ->
            if (b.label == WorkshopPanel.CLOSE_LABEL) {
                drawFitted(font, b.label, b.centerX, b.centerY + 9f, b.w - 24f)
            } else {
                val item = WorkshopCatalog.byId(b.label) ?: return@forEach
                val r = Workshop.rank(item.id)
                val pips = "●".repeat(r) + "○".repeat(item.maxRank - r)
                val price = if (r >= item.maxRank) "習得済" else "${WorkshopCatalog.cost(item, r)}片"
                drawFitted(font, "${item.title}　$pips　$price", b.centerX, b.centerY + 22f, b.w - 24f)
                font.color = cSub
                drawFitted(font, item.desc, b.centerX, b.centerY - 2f, b.w - 24f, scale = 0.85f)
                font.color = Color.WHITE
            }
        }
        batch.end()
    }

    /** v2.66: every switch persists together — one place, one habit. */
    private fun persistSettings() {
        try {
            val sp = Gdx.app.getPreferences("drift-settings")
            sp.putBoolean("soundOn", Sfx.enabled)
            sp.putBoolean("hapticsOn", Haptics.enabled)
            sp.putBoolean("leftHanded", leftyOn)
            sp.putBoolean("controlHintsOn", hintsOn)
            sp.putBoolean("loreHintsOn", loreOn)
            sp.flush()
        } catch (_: Throwable) { /* persist best-effort */ }
    }

    private fun toggleState(label: String): Boolean = when (label) {
        SettingsPanel.SOUND -> Sfx.enabled
        SettingsPanel.HAPTICS -> Haptics.enabled
        SettingsPanel.LEFTY -> leftyOn
        SettingsPanel.CONTROL_HINTS -> hintsOn
        else -> loreOn
    }

    /** v2.64 記録: dim + glass panel + the record lines + [起動診断をもう一度][閉じる]. */
    private fun drawRecords(w: Float, h: Float) {
        val lines = RecordsPanel.lines(
            Scores.bestWave, Scores.bestKills, Scores.simBestWave, Scores.simBestKills,
        ) { Achievements.has(it) }
        val btns = RecordsPanel.buttons(w, h)
        val px = 14f; val pw = w - 28f; val pTop = h * 0.90f; val pBot = h * 0.11f
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, 0.72f); shapes.rect(0f, 0f, w, h)
        shapes.color = Color(0.55f, 0.75f, 1f, 0.22f)
        shapes.rect(px - 1.5f, pBot - 1.5f, pw + 3f, (pTop - pBot) + 3f)
        shapes.color = Color(0.05f, 0.07f, 0.11f, 0.94f)
        shapes.rect(px, pBot, pw, pTop - pBot)
        btns.forEach { b ->
            shapes.color = Color(0.55f, 0.75f, 1f, 0.22f)
            shapes.rect(b.x - 1.5f, b.y - 1.5f, b.w + 3f, b.h + 3f)
            shapes.color = Color(0.09f, 0.12f, 0.18f, 0.95f)
            shapes.rect(b.x, b.y, b.w, b.h)
        }
        shapes.end()
        batch.begin()
        val font = Fonts.ui
        var y = pTop - 26f
        for (line in lines) {
            font.color = if (RecordsPanel.isHeader(line)) cSub else Color.WHITE
            drawFitted(font, line, w / 2f, y, pw - 24f)
            y -= 24f
        }
        font.color = cSub
        if (diagQueued) drawFitted(font, "✓ 次のランで起動診断を実行します", w / 2f, btns[0].y + btns[0].h + 26f, pw - 24f)
        font.color = Color.WHITE
        btns.forEach { b -> drawFitted(font, b.label, b.centerX, b.centerY + 7f, b.w - 16f) }
        batch.end()
    }

    /** Centered text that shrinks to fit [maxW] — the title screen's copy of Hud.fitText.
     *  v2.84: [scale] pre-shrinks (for the quiet secondary lines) before the fit kicks in. */
    private fun drawFitted(font: com.badlogic.gdx.graphics.g2d.BitmapFont, text: String, cx: Float, y: Float, maxW: Float, scale: Float = 1f) {
        val sx = font.data.scaleX; val sy = font.data.scaleY
        if (scale != 1f) font.data.setScale(sx * scale, sy * scale)
        glyph.setText(font, text)
        if (glyph.width > maxW) {
            val k = (maxW / glyph.width).coerceAtLeast(0.55f)
            font.data.setScale(sx * scale * k, sy * scale * k)
            glyph.setText(font, text)
        }
        font.draw(batch, glyph, cx - glyph.width / 2f, y)
        font.data.setScale(sx, sy)
    }

    private fun handleInput(buttons: List<UiButton>, rec: UiButton, set: UiButton, wsh: UiButton) {
        if (Gdx.input.justTouched()) {
            tmp.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
            viewport.unproject(tmp)
            if (showRecords) { // v2.64: the overlay swallows every tap while open
                val hit = RecordsPanel.buttons(viewport.worldWidth, viewport.worldHeight)
                    .firstOrNull { it.contains(tmp.x, tmp.y) } ?: return
                when (hit.label) {
                    RecordsPanel.REPLAY_LABEL -> {
                        diagQueued = true
                        try {
                            val sp = Gdx.app.getPreferences("drift-settings")
                            sp.putBoolean("tutorialDone", false)
                            sp.putBoolean("onboardDone", false)
                            sp.flush()
                        } catch (_: Throwable) { /* best-effort */ }
                        Sfx.play("scan")
                    }
                    RecordsPanel.CLOSE_LABEL -> showRecords = false
                }
                return
            }
            if (showWorkshop) { // v2.90: the workshop owns every tap while open
                val hit = WorkshopPanel.buttons(viewport.worldWidth, viewport.worldHeight)
                    .firstOrNull { it.contains(tmp.x, tmp.y) } ?: return
                if (hit.label == WorkshopPanel.CLOSE_LABEL) { showWorkshop = false; return }
                if (Workshop.buy(hit.label)) Sfx.play("levelup") else Sfx.play("hit")
                return
            }
            if (showSettings) { // v2.66: same rule — the panel owns every tap
                val hit = SettingsPanel.buttons(viewport.worldWidth, viewport.worldHeight)
                    .firstOrNull { it.contains(tmp.x, tmp.y) } ?: return
                when (hit.label) {
                    SettingsPanel.CLOSE_LABEL -> showSettings = false
                    SettingsPanel.SOUND -> {
                        Sfx.enabled = !Sfx.enabled
                        Ambience.setEnabled(Sfx.enabled) // v2.63: same switch quiets the ambience
                        persistSettings()
                    }
                    SettingsPanel.HAPTICS -> { Haptics.enabled = !Haptics.enabled; persistSettings() }
                    SettingsPanel.LEFTY -> { leftyOn = !leftyOn; persistSettings() }
                    SettingsPanel.CONTROL_HINTS -> { hintsOn = !hintsOn; persistSettings() }
                    SettingsPanel.LORE_HINTS -> { loreOn = !loreOn; persistSettings() }
                }
                return
            }
            if (rec.contains(tmp.x, tmp.y)) { // v2.64 記録
                showRecords = true
                Achievements.markSeen() // v2.73: opening the record clears the badge
                Sfx.play("scan")
                return
            }
            if (set.contains(tmp.x, tmp.y)) { // v2.66 設定
                showSettings = true
                Sfx.play("scan")
                return
            }
            if (wsh.contains(tmp.x, tmp.y)) { // v2.90 工房
                showWorkshop = true
                Sfx.play("scan")
                return
            }
            val hit = buttons.firstOrNull { it.contains(tmp.x, tmp.y) } ?: return
            when (hit.label) {
                "つづきから" -> app.startRun(fresh = false)
                "はじめから" -> app.startRun(fresh = true)
                "旧式戦闘訓練" -> app.startTraining()
            }
            return
        }
        if (showRecords || showSettings || showWorkshop) return // v2.64/66/90: keys don't start a run under an overlay
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER) ||
            Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE)
        ) {
            app.startRun(fresh = false) // continue if a save exists; otherwise a fresh run anyway
        }
    }

    override fun dispose() {
        if (::shapes.isInitialized) shapes.dispose()
        if (::batch.isInitialized) batch.dispose()
    }
}
