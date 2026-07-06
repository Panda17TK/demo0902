package io.github.panda17tk.arpg.save

import com.badlogic.gdx.Gdx

/** v2.93: how many times the keeper has completed the sync — the record panel wears it. */
object Endings {
    private const val PREFS = "drift-endings"

    var clears = 0
        private set

    fun load() {
        try { clears = Gdx.app.getPreferences(PREFS).getInteger("clears", 0) } catch (_: Throwable) { }
    }

    fun recordClear() {
        clears++
        try {
            val p = Gdx.app.getPreferences(PREFS)
            p.putInteger("clears", clears)
            p.flush()
        } catch (_: Throwable) { /* best-effort */ }
    }
}
