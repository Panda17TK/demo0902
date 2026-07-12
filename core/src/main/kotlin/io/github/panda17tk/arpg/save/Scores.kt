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
    private const val KEY_SIM_WAVE = "simBestWave"   // v2.62
    private const val KEY_SIM_KILLS = "simBestKills" // v2.62
    private const val KEY_CH_WEEK = "chWeek"         // v2.102 検証ラン
    private const val KEY_CH_WAVE = "chBestWave"
    private const val KEY_CH_KILLS = "chBestKills"
    private const val KEY_DAY = "dayKey"             // v2.180 今日の宙域
    private const val KEY_DAY_WAVE = "dayBestWave"
    private const val KEY_DAY_KILLS = "dayBestKills"

    var bestWave = 0
        private set
    var bestKills = 0
        private set

    // v2.62 訓練スコアボード: the simulation keeps its own ledger, walled off from the real run.
    var simBestWave = 0
        private set
    var simBestKills = 0
        private set

    // v2.102 検証ラン: this week's proving-run ledger — it wipes itself when the sky turns.
    var chWeek = 0L
        private set
    var chBestWave = 0
        private set
    var chBestKills = 0
        private set

    // v2.180 今日の宙域: the daily ledger — its own shelf, wiped at UTC midnight, so it never
    // tramples the weekly board (they share a screen, not a key).
    var dayKey = 0L
        private set
    var dayBestWave = 0
        private set
    var dayBestKills = 0
        private set

    fun load() {
        try {
            val p = Gdx.app.getPreferences(PREFS)
            bestWave = p.getInteger(KEY_WAVE, 0)
            bestKills = p.getInteger(KEY_KILLS, 0)
            simBestWave = p.getInteger(KEY_SIM_WAVE, 0)
            simBestKills = p.getInteger(KEY_SIM_KILLS, 0)
            chWeek = p.getLong(KEY_CH_WEEK, 0L)
            chBestWave = p.getInteger(KEY_CH_WAVE, 0)
            chBestKills = p.getInteger(KEY_CH_KILLS, 0)
            dayKey = p.getLong(KEY_DAY, 0L)
            dayBestWave = p.getInteger(KEY_DAY_WAVE, 0)
            dayBestKills = p.getInteger(KEY_DAY_KILLS, 0)
        } catch (_: Throwable) { /* no prefs backend → keep defaults */ }
    }

    /** v2.102: record a finished 検証ラン; a new week starts its ledger blank first. */
    fun recordChallenge(week: Long, wave: Int, kills: Int): Boolean {
        if (week != chWeek) { chWeek = week; chBestWave = 0; chBestKills = 0 }
        val newBest = wave > chBestWave
        if (newBest) chBestWave = wave
        if (kills > chBestKills) chBestKills = kills
        try {
            val p = Gdx.app.getPreferences(PREFS)
            p.putLong(KEY_CH_WEEK, chWeek)
            p.putInteger(KEY_CH_WAVE, chBestWave)
            p.putInteger(KEY_CH_KILLS, chBestKills)
            p.flush()
        } catch (_: Throwable) { /* persist best-effort */ }
        return newBest
    }

    /** v2.180: record a finished daily run; a new day starts its ledger blank first. */
    fun recordDaily(day: Long, wave: Int, kills: Int): Boolean {
        if (day != dayKey) { dayKey = day; dayBestWave = 0; dayBestKills = 0 }
        val newBest = wave > dayBestWave
        if (newBest) dayBestWave = wave
        if (kills > dayBestKills) dayBestKills = kills
        try {
            val p = Gdx.app.getPreferences(PREFS)
            p.putLong(KEY_DAY, dayKey)
            p.putInteger(KEY_DAY_WAVE, dayBestWave)
            p.putInteger(KEY_DAY_KILLS, dayBestKills)
            p.flush()
        } catch (_: Throwable) { /* persist best-effort */ }
        return newBest
    }

    /** v2.62: record a finished TRAINING run; returns true on a new sim-best wave. */
    fun recordSim(wave: Int, kills: Int): Boolean {
        val newBest = wave > simBestWave
        if (newBest) simBestWave = wave
        if (kills > simBestKills) simBestKills = kills
        try {
            val p = Gdx.app.getPreferences(PREFS)
            p.putInteger(KEY_SIM_WAVE, simBestWave)
            p.putInteger(KEY_SIM_KILLS, simBestKills)
            p.flush()
        } catch (_: Throwable) { /* persist best-effort */ }
        return newBest
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
