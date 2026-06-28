package io.github.panda17tk.arpg.input

import com.badlogic.gdx.Gdx

/** Thin, defensive wrapper over device vibration (Phase 8). No-ops where unsupported (desktop). */
object Haptics {
    fun buzz(ms: Int) {
        try { Gdx.input.vibrate(ms) } catch (_: Throwable) { /* no vibrator → ignore */ }
    }
}
