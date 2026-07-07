package io.github.panda17tk.arpg

import com.badlogic.gdx.Game
import com.badlogic.gdx.Screen
import io.github.panda17tk.arpg.audio.Ambience
import io.github.panda17tk.arpg.screens.GameScreen
import io.github.panda17tk.arpg.screens.TitleScreen

/** Root libGDX application. Boots the title screen (v2.58); screens switch through here. */
class App : Game() {
    override fun create() {
        setScreen(TitleScreen(this))
    }

    /** Enter the game — continuing the saved run, or abandoning it for a fresh one. */
    fun startRun(fresh: Boolean) {
        swapTo(GameScreen(startFresh = fresh))
    }

    /** Enter the game straight into the old-style combat simulation (v2.53). */
    fun startTraining() {
        swapTo(GameScreen(startInTraining = true))
    }

    /** v2.102 検証ラン: straight into this week's proving run (fixed sky, fixed loadout). */
    fun startChallenge() {
        swapTo(GameScreen(startInChallenge = true))
    }

    /** Back to the front door (the pause menu's タイトルへ). */
    fun showTitle() {
        swapTo(TitleScreen(this))
    }

    /** setScreen + dispose the screen we left — every switch builds a fresh screen instance. */
    private fun swapTo(next: Screen) {
        val old = screen
        setScreen(next)
        old?.dispose()
    }

    /** v2.63: the ambient loop lives across screens — only the app teardown stops it. */
    override fun dispose() {
        super.dispose()
        Ambience.dispose()
    }
}
