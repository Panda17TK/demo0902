package io.github.panda17tk.arpg.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import io.github.panda17tk.arpg.App
import io.github.panda17tk.arpg.core.Constants

/** Desktop launcher — DEVELOPMENT ONLY. The shipped product is the Android module. */
fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle(Constants.APP_TITLE)
        setWindowedMode(1280, 720)
        useVsync(true)
        setForegroundFPS(60)
    }
    Lwjgl3Application(App(), config)
}
