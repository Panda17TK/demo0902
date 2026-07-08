package io.github.panda17tk.arpg.save

import com.badlogic.gdx.Gdx

/**
 * v2.123 勤続記録 — the account-wide service tally: play time, kills, sorties, falls.
 * Real runs only. Time buffers in memory each frame (tick) and lands with the same seam
 * that folds the bestiary, so prefs are touched only at world boundaries. Defensive IO
 * like Scores — never throws, headless reads as zeros.
 */
object Stats {
    private const val PREFS = "drift-stats"

    var playSeconds = 0L; private set
    var kills = 0L; private set
    var sorties = 0L; private set
    var deaths = 0L; private set

    private var pendingTime = 0f

    fun load() {
        try {
            val p = Gdx.app.getPreferences(PREFS)
            playSeconds = p.getLong("playSeconds", 0L)
            kills = p.getLong("kills", 0L)
            sorties = p.getLong("sorties", 0L)
            deaths = p.getLong("deaths", 0L)
        } catch (_: Throwable) { /* headless / first boot → zeros stay */ }
    }

    /** One frame of real-run time; buffered, not persisted. */
    fun tick(delta: Float) { pendingTime += delta }

    /** A fresh real run leaves the hangar. */
    fun addSortie() { sorties += 1; persist() }

    /** Land the buffered time and this world's kills at a seam. */
    fun fold(killsByKind: Map<String, Int>) {
        val whole = pendingTime.toLong()
        playSeconds += whole
        pendingTime -= whole // the fraction carries to the next seam instead of being dropped
        kills += killsByKind.values.sumOf { it.toLong() }
        persist()
    }

    /** A real run ended on the floor (counted once, in the game-over one-shot). */
    fun addDeath() { deaths += 1; persist() }

    /** The record line's clock: total play time as H:MM. */
    fun clock(): String = "%d:%02d".format(playSeconds / 3600, (playSeconds % 3600) / 60)

    private fun persist() {
        try {
            val p = Gdx.app.getPreferences(PREFS)
            p.putLong("playSeconds", playSeconds)
            p.putLong("kills", kills)
            p.putLong("sorties", sorties)
            p.putLong("deaths", deaths)
            p.flush()
        } catch (_: Throwable) { /* persist best-effort */ }
    }
}
