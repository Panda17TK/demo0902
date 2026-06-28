package io.github.panda17tk.arpg.save

import com.badlogic.gdx.Gdx

/**
 * Persistent best-run record via libGDX Preferences (Phase 9), which maps to SharedPreferences on
 * Android and a properties file on desktop. Fully defensive: if Preferences are unavailable it
 * keeps in-memory values and never throws.
 */
object Scores {
    private const val PREFS = "arpg-scores"
    private const val KEY_WAVE = "bestWave"
    private const val KEY_KILLS = "bestKills"

    var bestWave = 0
        private set
    var bestKills = 0
        private set

    fun load() {
        try {
            val p = Gdx.app.getPreferences(PREFS)
            bestWave = p.getInteger(KEY_WAVE, 0)
            bestKills = p.getInteger(KEY_KILLS, 0)
        } catch (_: Throwable) { /* no prefs backend → keep defaults */ }
    }

    /** Record a finished run; returns true if it set a new best wave. */
    fun record(wave: Int, kills: Int): Boolean {
        val newBest = wave > bestWave
        if (newBest) bestWave = wave
        if (kills > bestKills) bestKills = kills
        try {
            val p = Gdx.app.getPreferences(PREFS)
            p.putInteger(KEY_WAVE, bestWave)
            p.putInteger(KEY_KILLS, bestKills)
            p.flush()
        } catch (_: Throwable) { /* persist best-effort */ }
        return newBest
    }
}
