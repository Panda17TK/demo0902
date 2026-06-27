package io.github.panda17tk.arpg

import com.badlogic.gdx.Game
import io.github.panda17tk.arpg.screens.GameScreen

/** Root libGDX application. Owns global services in later phases; for now boots GameScreen. */
class App : Game() {
    override fun create() {
        setScreen(GameScreen())
    }
}
