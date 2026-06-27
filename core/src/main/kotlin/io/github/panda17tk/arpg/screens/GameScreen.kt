package io.github.panda17tk.arpg.screens

import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
import io.github.panda17tk.arpg.core.Constants

/** Placeholder screen: proves the render loop runs on every platform target. */
class GameScreen : ScreenAdapter() {
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont

    override fun show() {
        batch = SpriteBatch()
        font = BitmapFont() // default ASCII font; Japanese (FreeType) arrives in Phase 8
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(0.06f, 0.07f, 0.10f, 1f)
        batch.begin()
        font.draw(batch, "${Constants.APP_TITLE} - Phase 0 OK", 24f, 48f)
        batch.end()
    }

    override fun dispose() {
        batch.dispose()
        font.dispose()
    }
}
