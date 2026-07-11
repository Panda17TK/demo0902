package io.github.panda17tk.arpg.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter

/**
 * Japanese-capable fonts generated at runtime from a bundled OFL TTF via gdx-freetype.
 * `incremental = true` renders glyphs lazily as strings are drawn, so the whole CJK set never
 * needs enumerating. Defensive: if the native lib or the TTF is unavailable, falls back to the
 * built-in ASCII BitmapFont so the game still runs (labels show as boxes but never crash).
 */
object Fonts {
    private const val FONT_PATH = "fonts/DotGothic16-Regular.ttf"
    private var generator: FreeTypeFontGenerator? = null

    lateinit var ui: BitmapFont
        private set
    lateinit var title: BitmapFont
        private set

    /** [uiScale] is the device density so glyphs are baked large enough to read in dp-unit space. */
    fun load(uiScale: Float = 1f) {
        dispose() // v2.168 安全な帰還: retire the previous pair + generator — every screen swap
        // used to leak a FreeType generator and two fonts (screens must NOT dispose these)
        try {
            val handle = Gdx.files.internal(FONT_PATH)
            if (handle.exists()) {
                val gen = FreeTypeFontGenerator(handle)
                generator = gen
                // Bake hi-res for crispness, but scale down so the font *measures* its base size
                // in the dp-unit HUD (the viewport re-applies density). Avoids double-scaling.
                ui = gen.generateFont(param((20f * uiScale).toInt().coerceAtLeast(12))).apply { data.setScale(1f / uiScale) }
                title = gen.generateFont(param((44f * uiScale).toInt().coerceAtLeast(24))).apply { data.setScale(1f / uiScale) }
                return
            }
        } catch (_: Throwable) { /* no freetype native / no TTF → ASCII fallback */ }
        ui = BitmapFont()
        title = BitmapFont().apply { data.setScale(2f) }
    }

    private fun param(sz: Int) = FreeTypeFontParameter().apply {
        size = sz
        incremental = true
        minFilter = Texture.TextureFilter.Linear
        magFilter = Texture.TextureFilter.Linear
        color = Color.WHITE
    }

    fun dispose() {
        if (::ui.isInitialized) runCatching { ui.dispose() }
        if (::title.isInitialized) runCatching { title.dispose() }
        generator?.let { runCatching { it.dispose() } }
        generator = null
    }
}
