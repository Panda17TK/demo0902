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
import io.github.panda17tk.arpg.save.PreferencesRunSaveStore
import io.github.panda17tk.arpg.save.Scores
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

    // A deterministic drifting star field: fraction positions + parallax speed per star.
    private data class Star(val fx: Float, val fy: Float, val size: Float, val speed: Float)
    private val stars: List<Star> = run {
        val r = Rng(7L)
        List(110) { Star(r.nextFloat(), r.nextFloat(), 0.8f + r.nextFloat() * 1.8f, 2f + r.nextFloat() * 10f) }
    }

    private val cStar = Color(0.85f, 0.9f, 1f, 0.7f)
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
        try { // v2.59 設定: restore the sound / haptics switches
            val sp = Gdx.app.getPreferences("drift-settings")
            Sfx.enabled = sp.getBoolean("soundOn", true)
            Haptics.enabled = sp.getBoolean("hapticsOn", true)
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
        // drifting stars (wrap horizontally; slow parallax by size)
        for (s in stars) {
            val x = (s.fx * w + t * s.speed) % w
            shapes.color = cStar
            shapes.circle(x, s.fy * h, s.size, 6)
        }
        // a dark planet rising from the bottom-left — the server-world, waiting
        shapes.color = cPlanetRim
        shapes.circle(w * 0.12f, -h * 0.28f, h * 0.46f, 64)
        shapes.color = cPlanet
        shapes.circle(w * 0.12f, -h * 0.28f, h * 0.45f, 64)
        // a far jump-gate ring, breathing
        val breath = 0.5f + 0.5f * kotlin.math.sin(t * 0.8f)
        shapes.color = cGate
        shapes.circle(w * 0.82f, h * 0.80f, 16f + 3f * breath, 24)
        shapes.color = Color(0.8f, 0.97f, 1f, 0.8f)
        shapes.circle(w * 0.82f, h * 0.80f, 3f, 10)
        // menu buttons (glass cards) + the settings toggle pair
        val toggles = TitleLayout.toggles(w, h)
        (buttons + toggles).forEach { b ->
            shapes.color = Color(0.55f, 0.75f, 1f, 0.22f)
            shapes.rect(b.x - 1.5f, b.y - 1.5f, b.w + 3f, b.h + 3f)
            shapes.color = Color(0.05f, 0.07f, 0.11f, 0.85f)
            shapes.rect(b.x, b.y, b.w, b.h)
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
        glyph.setText(font, "慣性で漂う宇宙と、あなたを覚えている星々。")
        font.draw(batch, glyph, (w - glyph.width) / 2f, h * 0.63f)
        glyph.setText(font, "— 稼働中の人類保全装置群、その最後の保守員 —")
        font.draw(batch, glyph, (w - glyph.width) / 2f, h * 0.59f)
        font.color = Color.WHITE
        buttons.forEach { b ->
            glyph.setText(font, b.label)
            font.draw(batch, glyph, b.centerX - glyph.width / 2f, b.centerY + glyph.height / 2f)
        }
        font.color = cSub
        toggles.forEachIndexed { i, b ->
            val on = if (i == 0) Sfx.enabled else Haptics.enabled
            glyph.setText(font, "${b.label}: ${if (on) "ON" else "OFF"}")
            font.draw(batch, glyph, b.centerX - glyph.width / 2f, b.centerY + glyph.height / 2f)
        }
        font.color = Color.WHITE
        font.color = cSub
        if (Scores.simBestWave > 0) { // v2.62 訓練スコアボード
            glyph.setText(font, "訓練記録　ウェーブ ${Scores.simBestWave}　撃破 ${Scores.simBestKills}")
            font.draw(batch, glyph, (w - glyph.width) / 2f, h * 0.12f)
        }
        glyph.setText(font, "外部同期: 停止　ローカル保全モード: 稼働")
        font.draw(batch, glyph, (w - glyph.width) / 2f, h * 0.08f)
        font.color = Color.WHITE
        batch.end()

        handleInput(buttons, toggles)
    }

    private fun handleInput(buttons: List<UiButton>, toggles: List<UiButton>) {
        if (Gdx.input.justTouched()) {
            tmp.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
            viewport.unproject(tmp)
            // v2.59 設定: the toggle pair flips + persists in place
            toggles.forEachIndexed { i, b ->
                if (b.contains(tmp.x, tmp.y)) {
                    if (i == 0) {
                        Sfx.enabled = !Sfx.enabled
                        Ambience.setEnabled(Sfx.enabled) // v2.63: same switch quiets the ambience
                    } else Haptics.enabled = !Haptics.enabled
                    try {
                        val sp = Gdx.app.getPreferences("drift-settings")
                        sp.putBoolean("soundOn", Sfx.enabled)
                        sp.putBoolean("hapticsOn", Haptics.enabled)
                        sp.flush()
                    } catch (_: Throwable) { /* persist best-effort */ }
                    return
                }
            }
            val hit = buttons.firstOrNull { it.contains(tmp.x, tmp.y) } ?: return
            when (hit.label) {
                "つづきから" -> app.startRun(fresh = false)
                "はじめから" -> app.startRun(fresh = true)
                "旧式戦闘訓練" -> app.startTraining()
            }
            return
        }
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
