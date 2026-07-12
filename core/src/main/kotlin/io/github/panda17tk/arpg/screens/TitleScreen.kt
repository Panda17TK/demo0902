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
import io.github.panda17tk.arpg.save.SaveSlots
import io.github.panda17tk.arpg.ui.SlotPanel
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
    private var recordsBestiary = false // v2.113 図鑑: the record's second page
    private var recordsBestiaryPage = 0 // v2.120: which spread of the book is open
    private var recordsLore = false // v2.182 図録: the curated codex reader, opened from the bestiary grid
    private var recordsLorePage = 0
    private var recordsHandover = false // v2.122 引き継ぎ: the record's transfer page
    private var recordsAch = false      // v2.124 実績: the record's title spread
    private var recordsAchPage = 0
    private var handoverNote: String? = null // v2.122: the last export/import outcome
    private var diagQueued = false  // v2.64: 起動診断をもう一度 was pressed this visit
    private var showSettings = false // v2.66 設定: the settings-panel overlay
    private var showWorkshop = false // v2.90 工房: the workshop overlay
    private var leftyOn = false     // v2.65 左利き配置 (applied by the game screen on entry)
    private var hintsOn = true      // v2.66 操作ヒント (applied by the game screen on entry)
    private var loreOn = true       // v2.66 世界観ヒント (applied by the game screen on entry)
    private var shakeOn = true      // v2.96 画面の揺れ
    private var softFlash = false   // v2.96 閃光をやわらげる
    private var assistOn = true     // v2.112 エイム補助 (applied by the game screen on entry)
    private var langEn = false      // v2.115 English表示 (presentation only — Lang.tr at draw time)
    private var volume = 1f         // v2.96 音量 (0/0.25/0.5/0.75/1)
    private var difficulty = io.github.panda17tk.arpg.sim.Difficulty.NORMAL // v2.97
    private var showSlots = false  // v2.103 セーブスロット: the journey picker
    private var slotsFresh = false // v2.103: picker opened from はじめから (any slot starts fresh)
    private var showTunePad = false // v2.98 調整モード: the passcode pad
    private var tuneCode = ""       // v2.98: the digits typed so far (masked on screen)

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
    private val cCrash = Color(1f, 0.5f, 0.45f, 0.85f) // v2.168 安全な帰還
    private var lastCrash = "" // v2.168: the black box — what the previous session died of

    override fun show() {
        shapes = ShapeRenderer()
        batch = SpriteBatch()
        val uiScale = Gdx.graphics.density.coerceIn(1f, 4f)
        Fonts.load(uiScale)
        viewport = ScreenViewport()
        viewport.setUnitsPerPixel(1f / uiScale)
        viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
        hasSave = SaveSlots.hasAny() // v2.103: any journey lights つづきから
        Scores.load() // v2.62: the training scoreboard shows on the front door
        Achievements.load() // v2.64 記録: the service record reads from here
        Workshop.load() // v2.90 工房: the ledger and its ranks
        io.github.panda17tk.arpg.save.Endings.load() // v2.93: the completed syncs
        io.github.panda17tk.arpg.save.Bestiary.load() // v2.113 図鑑
        io.github.panda17tk.arpg.save.Stats.load() // v2.123 勤続記録
        loadSettings() // v2.122: shared with the transfer page's import
        Ambience.setMaster(volume)
        Ambience.setEnabled(Sfx.enabled) // v2.63: the サウンド toggle gates the ambient loop too
        Ambience.play(AmbientTrack.TITLE)
    }

    /** v2.59 設定 / v2.122 引き継ぎ: read every device setting into this screen's state. */
    private fun loadSettings() {
        try {
            val sp = Gdx.app.getPreferences("drift-settings")
            Sfx.enabled = sp.getBoolean("soundOn", true)
            Haptics.enabled = sp.getBoolean("hapticsOn", true)
            leftyOn = sp.getBoolean("leftHanded", false) // v2.65
            hintsOn = sp.getBoolean("controlHintsOn", true) // v2.66
            loreOn = sp.getBoolean("loreHintsOn", false)    // v2.66 / v2.173: the asides start silent
            shakeOn = sp.getBoolean("shakeOn", true)        // v2.96
            softFlash = sp.getBoolean("softFlash", false)   // v2.96
            assistOn = sp.getBoolean("aimAssist", true)     // v2.112
            langEn = sp.getBoolean("langEn", false)         // v2.115
            volume = sp.getFloat("masterVolume", 1f).coerceIn(0f, 1f) // v2.96
            difficulty = io.github.panda17tk.arpg.sim.Difficulty.byName(sp.getString("difficulty", "NORMAL")) // v2.97
            io.github.panda17tk.arpg.save.OceanDensity.tier = sp.getInteger("oceanDensity", io.github.panda17tk.arpg.save.OceanDensity.MEDIUM).coerceIn(0, 2) // v2.165
            io.github.panda17tk.arpg.save.PerfHud.enabled = sp.getBoolean("perfHud", false) // v2.167
            lastCrash = sp.getString("lastCrash", "") // v2.168 安全な帰還
        } catch (_: Throwable) { /* defaults stay on */ }
        io.github.panda17tk.arpg.i18n.Lang.en = langEn // v2.115: the dictionary follows the pref
        Sfx.volume = volume
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
        val dif = TitleLayout.difficultyButton(w, h, difficulty.label) // v2.97
        val tun = TitleLayout.tuneButton(w, h, if (io.github.panda17tk.arpg.save.TuneMode.active) "調整◉" else "調整") // v2.98
        val dly = TitleLayout.dailyButton(w, h) // v2.180 今日の宙域
        (buttons + rec + set + wsh + dif + tun + dly).forEach { b ->
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
        // v2.104 周回の印: one thin gold ring per completed sync, worn around the logo's halo.
        val clearRings = minOf(io.github.panda17tk.arpg.save.Endings.clears, 5)
        if (clearRings > 0) {
            shapes.begin(ShapeRenderer.ShapeType.Line)
            for (k in 0 until clearRings) {
                cTwinkle.set(1f, 0.84f, 0.45f, 0.42f - 0.05f * k)
                shapes.color = cTwinkle
                shapes.circle(w / 2f, h * 0.72f, minDim * (0.235f + 0.020f * k), 64)
            }
            shapes.end()
        }

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
        (buttons + rec + set + wsh + dif + tun + dly).forEach { b ->
            glyph.setText(font, io.github.panda17tk.arpg.i18n.Lang.tr(b.label))
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
        if (showTunePad) drawTunePad(w, h)   // v2.98: the passcode pad over everything
        if (showSlots) drawSlots(w, h)       // v2.103: the journey picker over everything
        if (lastCrash.isNotEmpty()) { // v2.168: the previous session's crash, for the report
            batch.begin()
            val cf = Fonts.ui
            cf.color = cCrash
            drawFitted(cf, "前回のエラー: $lastCrash", w / 2f, 14f, w - 20f, scale = 0.7f)
            cf.color = Color.WHITE
            batch.end()
        }
        handleInput(buttons, rec, set, wsh, dif, tun)
    }

    /** v2.103 セーブスロット: dim + three journey plates (occupied shows its summary) + 閉じる. */
    private fun drawSlots(w: Float, h: Float) {
        val rows = SlotPanel.rows(w, h)
        val close = SlotPanel.closeButton(w, h)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, 0.72f); shapes.rect(0f, 0f, w, h)
        (rows + close).forEach { b ->
            shapes.color = Color(0.55f, 0.75f, 1f, 0.22f)
            shapes.rect(b.x - 1.5f, b.y - 1.5f, b.w + 3f, b.h + 3f)
            shapes.color = Color(0.05f, 0.07f, 0.11f, 0.94f)
            shapes.rect(b.x, b.y, b.w, b.h)
        }
        shapes.end()
        batch.begin()
        val font = Fonts.ui
        font.color = cSub
        drawFitted(font, if (slotsFresh) "どの枠ではじめるか（上書きされる）" else "どの旅をつづけるか", w / 2f, h * 0.72f, w - 48f)
        font.color = Color.WHITE
        rows.forEachIndexed { i, r ->
            val summary = SaveSlots.summary(i)
            if (summary == null && !slotsFresh) font.color = cSub // nothing to continue — muted
            drawFitted(font, "スロット${i + 1}　${summary ?: "空き"}", r.centerX, r.centerY + 8f, r.w - 24f)
            font.color = Color.WHITE
        }
        glyph.setText(font, io.github.panda17tk.arpg.i18n.Lang.tr(close.label))
        font.draw(batch, glyph, close.centerX - glyph.width / 2f, close.centerY + glyph.height / 2f)
        batch.end()
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
            val text = when (b.label) {
                SettingsPanel.CLOSE_LABEL -> b.label
                SettingsPanel.VOLUME -> "${io.github.panda17tk.arpg.i18n.Lang.tr(SettingsPanel.VOLUME)}: ${(volume * 100).toInt()}%" // v2.96: a cycle, not a toggle
                SettingsPanel.OCEAN -> "${io.github.panda17tk.arpg.i18n.Lang.tr(SettingsPanel.OCEAN)}: ${io.github.panda17tk.arpg.i18n.Lang.tr(io.github.panda17tk.arpg.save.OceanDensity.label())}" // v2.165
                else -> "${io.github.panda17tk.arpg.i18n.Lang.tr(b.label)}: ${if (toggleState(b.label)) "ON" else "OFF"}"
            }
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

    /** v2.98 調整モード: dim + the masked code + a 3x4 passcode pad. */
    private fun drawTunePad(w: Float, h: Float) {
        val btns = io.github.panda17tk.arpg.ui.TunePad.buttons(w, h)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = Color(0f, 0f, 0f, 0.78f); shapes.rect(0f, 0f, w, h)
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
        drawFitted(font, "調整モード — パスコード", w / 2f, h * 0.74f, w - 48f)
        font.color = Color.WHITE
        drawFitted(font, if (tuneCode.isEmpty()) "----" else "●".repeat(tuneCode.length), w / 2f, h * 0.68f, w - 48f)
        btns.forEach { b ->
            glyph.setText(font, b.label)
            font.draw(batch, glyph, b.centerX - glyph.width / 2f, b.centerY + glyph.height / 2f)
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
                val cap = WorkshopCatalog.rankCap(item, io.github.panda17tk.arpg.save.Endings.clears) // v2.104
                val price = when {
                    r >= item.maxRank -> "習得済"
                    r >= cap -> "同期完了で解放"
                    else -> "${WorkshopCatalog.cost(item, r)}片"
                }
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
            sp.putBoolean("shakeOn", shakeOn)      // v2.96
            sp.putBoolean("softFlash", softFlash)  // v2.96
            sp.putBoolean("aimAssist", assistOn)   // v2.112
            sp.putBoolean("langEn", langEn)        // v2.115
            sp.putFloat("masterVolume", volume)    // v2.96
            sp.putString("difficulty", difficulty.name) // v2.97
            sp.putInteger("oceanDensity", io.github.panda17tk.arpg.save.OceanDensity.tier) // v2.165
            sp.putBoolean("perfHud", io.github.panda17tk.arpg.save.PerfHud.enabled) // v2.167
            sp.flush()
        } catch (_: Throwable) { /* persist best-effort */ }
    }

    private fun toggleState(label: String): Boolean = when (label) {
        SettingsPanel.SOUND -> Sfx.enabled
        SettingsPanel.HAPTICS -> Haptics.enabled
        SettingsPanel.LEFTY -> leftyOn
        SettingsPanel.CONTROL_HINTS -> hintsOn
        SettingsPanel.SHAKE -> shakeOn      // v2.96
        SettingsPanel.SOFT_FLASH -> softFlash // v2.96
        SettingsPanel.AIM_ASSIST -> assistOn   // v2.112
        SettingsPanel.LANGUAGE -> langEn       // v2.115
        SettingsPanel.PERF -> io.github.panda17tk.arpg.save.PerfHud.enabled // v2.167
        else -> loreOn
    }

    /** v2.64 記録: dim + glass panel + the record lines + [起動診断をもう一度][閉じる]. */
    private fun drawRecords(w: Float, h: Float) {
        val lines = if (recordsHandover) { // v2.122 引き継ぎ
            RecordsPanel.handoverLines() + listOfNotNull(handoverNote)
        } else if (recordsBestiary) { // v2.113 図鑑: the record's second page
            RecordsPanel.bestiaryLines({ io.github.panda17tk.arpg.save.Bestiary.count(it) }, recordsBestiaryPage)
        } else if (recordsLore) { // v2.182 図録: the curated codex reader
            RecordsPanel.loreLines({ io.github.panda17tk.arpg.save.Bestiary.count(it) }, recordsLorePage)
        } else if (recordsAch) { // v2.124 実績: the title spread
            RecordsPanel.achievementLines(recordsAchPage) { Achievements.has(it) }
        } else RecordsPanel.lines(
            Scores.bestWave, Scores.bestKills, Scores.simBestWave, Scores.simBestKills,
            clears = io.github.panda17tk.arpg.save.Endings.clears, // v2.93
            chWeek = Scores.chWeek, chWave = Scores.chBestWave, chKills = Scores.chBestKills, // v2.102
            chDaysLeft = io.github.panda17tk.arpg.save.Challenge.daysLeft(System.currentTimeMillis()), // v2.119
            dayKey = Scores.dayKey, dayWave = Scores.dayBestWave, dayKills = Scores.dayBestKills, // v2.180
            dayIsToday = Scores.dayKey == io.github.panda17tk.arpg.save.Challenge.dayOf(System.currentTimeMillis()),
            stClock = io.github.panda17tk.arpg.save.Stats.clock(), stKills = io.github.panda17tk.arpg.save.Stats.kills, stSorties = io.github.panda17tk.arpg.save.Stats.sorties, // v2.123
            bestiaryKnown = io.github.panda17tk.arpg.save.Bestiary.knownCount(), // v2.124
        ) { Achievements.has(it) }
        val btns = when {
            recordsHandover -> RecordsPanel.handoverButtons(w, h)
            recordsBestiary -> RecordsPanel.bestiaryButtons(w, h) // v2.182: the grid foot gains 図録
            else -> RecordsPanel.buttons(w, h, recordsLore || recordsAch) // lore/ach share the pager form
        }
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
        val step = ((pTop - pBot - 130f) / lines.size).coerceIn(15f, 24f) // v2.113: long pages tighten, never spill
        for (line in lines) {
            font.color = if (RecordsPanel.isHeader(line)) cSub else Color.WHITE
            drawFitted(font, line, w / 2f, y, pw - 24f, scale = if (step < 20f) 0.85f else 1f)
            y -= step
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
        val shown = io.github.panda17tk.arpg.i18n.Lang.tr(text) // v2.115: the title's labels translate at the funnel
        val sx = font.data.scaleX; val sy = font.data.scaleY
        if (scale != 1f) font.data.setScale(sx * scale, sy * scale)
        glyph.setText(font, shown)
        if (glyph.width > maxW) {
            val k = (maxW / glyph.width).coerceAtLeast(0.55f)
            font.data.setScale(sx * scale * k, sy * scale * k)
            glyph.setText(font, shown)
        }
        font.draw(batch, glyph, cx - glyph.width / 2f, y)
        font.data.setScale(sx, sy)
    }

    private fun handleInput(buttons: List<UiButton>, rec: UiButton, set: UiButton, wsh: UiButton, dif: UiButton, tun: UiButton) {
        if (Gdx.input.justTouched()) {
            tmp.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
            viewport.unproject(tmp)
            if (showSlots) { // v2.103: the slot picker owns every tap while open
                val pw = viewport.worldWidth; val ph = viewport.worldHeight
                if (SlotPanel.closeButton(pw, ph).contains(tmp.x, tmp.y)) { showSlots = false; return }
                val idx = SlotPanel.rows(pw, ph).indexOfFirst { it.contains(tmp.x, tmp.y) }
                if (idx >= 0) {
                    val occupied = SaveSlots.summary(idx) != null
                    if (slotsFresh || occupied) {
                        Sfx.play("scan")
                        app.startRun(fresh = slotsFresh, slot = idx)
                    } else {
                        Sfx.play("hit") // an empty slot holds no journey to continue
                    }
                }
                return
            }
            if (showRecords) { // v2.64: the overlay swallows every tap while open
                val hit = (when {
                    recordsHandover -> RecordsPanel.handoverButtons(viewport.worldWidth, viewport.worldHeight)
                    recordsBestiary -> RecordsPanel.bestiaryButtons(viewport.worldWidth, viewport.worldHeight) // v2.182
                    else -> RecordsPanel.buttons(viewport.worldWidth, viewport.worldHeight, recordsLore || recordsAch)
                }).firstOrNull { it.contains(tmp.x, tmp.y) } ?: return
                when (hit.label) {
                    RecordsPanel.BESTIARY_LABEL -> { recordsBestiary = true; recordsBestiaryPage = 0; Sfx.play("scan") } // v2.113
                    RecordsPanel.ACHIEVEMENTS_LABEL -> { recordsAch = true; recordsAchPage = 0; Sfx.play("scan") } // v2.124
                    RecordsPanel.LORE_LABEL -> { recordsLore = true; recordsBestiary = false; recordsLorePage = 0; Sfx.play("scan") } // v2.182: grid → codex
                    RecordsPanel.BACK_LABEL -> { // v2.182: from the codex, back to the grid; else back to 記録
                        if (recordsLore) { recordsLore = false; recordsBestiary = true; recordsLorePage = 0 }
                        else { recordsBestiary = false; recordsBestiaryPage = 0; recordsHandover = false; recordsAch = false; recordsAchPage = 0 }
                        Sfx.play("scan")
                    }
                    RecordsPanel.HANDOVER_LABEL -> { recordsHandover = true; handoverNote = null; Sfx.play("scan") } // v2.122
                    RecordsPanel.EXPORT_LABEL -> { // v2.122: the account → one block of text
                        val text = io.github.panda17tk.arpg.save.Handover.export()
                        handoverNote = if (text != null) {
                            try { Gdx.app.clipboard.contents = text } catch (_: Throwable) { }
                            "クリップボードへ書き出した（${text.length}文字）"
                        } else "書き出せなかった"
                        Sfx.play("scan")
                    }
                    RecordsPanel.IMPORT_LABEL -> { // v2.122: one block of text → the account
                        val text = try { Gdx.app.clipboard.contents ?: "" } catch (_: Throwable) { "" }
                        handoverNote = if (io.github.panda17tk.arpg.save.Handover.import(text)) {
                            loadSettings() // the imported settings take effect here too
                            hasSave = SaveSlots.hasAny()
                            "取り込んだ — 記録と設定に反映した"
                        } else "取り込めなかった — クリップボードに引き継ぎ文がない"
                        Sfx.play(if (handoverNote!!.startsWith("取り込んだ")) "levelup" else "hit")
                    }
                    "前へ", "次へ" -> { // v2.120/v2.124/v2.182: leaf through whichever spread is open
                        val pages = when {
                            recordsAch -> RecordsPanel.achievementPages()
                            recordsLore -> RecordsPanel.lorePages { io.github.panda17tk.arpg.save.Bestiary.count(it) }
                            else -> RecordsPanel.bestiaryPages(RecordsPanel.kindCount())
                        }
                        val step = if (hit.label == "次へ") 1 else pages - 1
                        when {
                            recordsAch -> recordsAchPage = (recordsAchPage + step) % pages
                            recordsLore -> recordsLorePage = (recordsLorePage + step) % pages
                            recordsBestiary -> recordsBestiaryPage = (recordsBestiaryPage + step) % pages
                        }
                        Sfx.play("scan")
                    }
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
                    RecordsPanel.CLOSE_LABEL -> { showRecords = false; recordsBestiary = false; recordsBestiaryPage = 0; recordsHandover = false; recordsAch = false; recordsAchPage = 0; recordsLore = false; recordsLorePage = 0 }
                }
                return
            }
            if (showTunePad) { // v2.98: the pad owns every tap; tapping past it closes it
                val hit = io.github.panda17tk.arpg.ui.TunePad.buttons(viewport.worldWidth, viewport.worldHeight)
                    .firstOrNull { it.contains(tmp.x, tmp.y) }
                if (hit == null) { showTunePad = false; tuneCode = ""; return }
                when (hit.label) {
                    io.github.panda17tk.arpg.ui.TunePad.ERASE -> tuneCode = tuneCode.dropLast(1)
                    io.github.panda17tk.arpg.ui.TunePad.ENTER -> {
                        if (io.github.panda17tk.arpg.save.TuneMode.tryUnlock(tuneCode)) {
                            showTunePad = false; tuneCode = ""
                            Sfx.play("levelup")
                        } else {
                            tuneCode = ""
                            Sfx.play("hit")
                        }
                    }
                    else -> if (tuneCode.length < 8) tuneCode += hit.label
                }
                return
            }
            if (showWorkshop) { // v2.90: the workshop owns every tap while open
                val hit = WorkshopPanel.buttons(viewport.worldWidth, viewport.worldHeight)
                    .firstOrNull { it.contains(tmp.x, tmp.y) } ?: return
                if (hit.label == WorkshopPanel.CLOSE_LABEL) { showWorkshop = false; return }
                if (Workshop.buy(hit.label)) {
                    Sfx.play("levelup")
                    // v2.92: the first craft — and a mastered one — join the service record.
                    Achievements.unlock(io.github.panda17tk.arpg.save.Achievement.WORKSHOP_PATRON)
                    WorkshopCatalog.byId(hit.label)?.let { item ->
                        if (Workshop.rank(item.id) >= item.maxRank) {
                            Achievements.unlock(io.github.panda17tk.arpg.save.Achievement.WORKSHOP_MASTER)
                        }
                    }
                } else Sfx.play("hit")
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
                    SettingsPanel.VOLUME -> { // v2.96: cycle down, wrap to full; the ping previews it
                        volume = if (volume <= 0f) 1f else ((volume * 4).toInt() - 1) / 4f
                        Sfx.volume = volume
                        Ambience.setMaster(volume)
                        persistSettings()
                        Sfx.play("scan")
                    }
                    SettingsPanel.SHAKE -> { shakeOn = !shakeOn; persistSettings() }      // v2.96
                    SettingsPanel.SOFT_FLASH -> { softFlash = !softFlash; persistSettings() } // v2.96
                    SettingsPanel.AIM_ASSIST -> { assistOn = !assistOn; persistSettings() } // v2.112
                    SettingsPanel.LANGUAGE -> { langEn = !langEn; io.github.panda17tk.arpg.i18n.Lang.en = langEn; persistSettings() } // v2.115
                    SettingsPanel.OCEAN -> { // v2.165: a cycle like the volume — 高→中→低
                        io.github.panda17tk.arpg.save.OceanDensity.tier = io.github.panda17tk.arpg.save.OceanDensity.next()
                        persistSettings()
                        Sfx.play("scan")
                    }
                    SettingsPanel.PERF -> { io.github.panda17tk.arpg.save.PerfHud.enabled = !io.github.panda17tk.arpg.save.PerfHud.enabled; persistSettings() } // v2.167
                }
                return
            }
            val dlyBtn = TitleLayout.dailyButton(viewport.worldWidth, viewport.worldHeight) // v2.180 今日の宙域
            if (dlyBtn.contains(tmp.x, tmp.y)) {
                app.startDailyChallenge()
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
            if (dif.contains(tmp.x, tmp.y)) { // v2.97 難易度: taps cycle the run mode
                difficulty = difficulty.next()
                persistSettings()
                Sfx.play("scan")
                return
            }
            if (tun.contains(tmp.x, tmp.y)) { // v2.98 調整: active toggles off, else the pad opens
                if (io.github.panda17tk.arpg.save.TuneMode.active) {
                    io.github.panda17tk.arpg.save.TuneMode.active = false
                    Sfx.play("scan")
                } else {
                    showTunePad = true
                    tuneCode = ""
                    Sfx.play("scan")
                }
                return
            }
            val hit = buttons.firstOrNull { it.contains(tmp.x, tmp.y) } ?: return
            when (hit.label) {
                "つづきから" -> { showSlots = true; slotsFresh = false; Sfx.play("scan") } // v2.103
                "はじめから" -> { showSlots = true; slotsFresh = true; Sfx.play("scan") } // v2.103
                "旧式戦闘訓練" -> app.startTraining()
                "検証ラン" -> app.startChallenge() // v2.102 今週の宙域
            }
            return
        }
        if (showRecords || showSettings || showWorkshop || showTunePad || showSlots) return // v2.64/66/90/98: keys don't start a run under an overlay
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER) ||
            Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE)
        ) {
            app.startRun(fresh = false, slot = SaveSlots.firstUsed() ?: 0) // v2.103: the first journey
        }
    }

    override fun dispose() {
        if (::shapes.isInitialized) shapes.dispose()
        if (::batch.isInitialized) batch.dispose()
    }
}
