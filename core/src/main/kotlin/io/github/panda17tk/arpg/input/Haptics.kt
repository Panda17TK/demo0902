package io.github.panda17tk.arpg.input

import com.badlogic.gdx.Gdx

/** Thin, defensive wrapper over device vibration (Phase 8). No-ops where unsupported (desktop). */
object Haptics {
    /** v2.59 設定: master switch (persisted by the title screen's toggle). */
    var enabled = true

    fun buzz(ms: Int) {
        if (!enabled) return
        try { Gdx.input.vibrate(ms) } catch (_: Throwable) { /* no vibrator → ignore */ }
    }
}
