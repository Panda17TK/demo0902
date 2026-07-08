package io.github.panda17tk.arpg.save

import com.badlogic.gdx.Gdx

/**
 * v2.113 討伐図鑑 — the account-wide tally of what the keeper has put down, by enemy kind.
 * The sim counts per-world into GameOver.killsByKind (deterministic); the SCREEN folds those
 * tallies in here at world seams (transition / training entry / game over / quit-to-title).
 * Defensive prefs IO like Scores — never throws, headless reads as empty.
 */
object Bestiary {
    private const val PREFS = "drift-bestiary"

    private val tallies = HashMap<String, Int>()

    fun load() {
        tallies.clear()
        try {
            val p = Gdx.app.getPreferences(PREFS)
            for ((k, v) in p.get()) {
                (v as? String)?.toIntOrNull()?.let { if (it > 0) tallies[k.toString()] = it }
            }
        } catch (_: Throwable) { /* headless / first boot → empty book */ }
    }

    fun count(kind: String): Int = tallies[kind] ?: 0

    /** How many kinds carry at least one mark. */
    fun knownCount(): Int = tallies.count { it.value > 0 }

    /** Fold one world's tallies into the book (called at world seams; no-op when empty). */
    fun record(killsByKind: Map<String, Int>) {
        if (killsByKind.isEmpty()) return
        for ((k, n) in killsByKind) if (n > 0) tallies[k] = (tallies[k] ?: 0) + n
        try {
            val p = Gdx.app.getPreferences(PREFS)
            for ((k, n) in tallies) p.putString(k, n.toString())
            p.flush()
        } catch (_: Throwable) { /* persist best-effort */ }
    }
}
