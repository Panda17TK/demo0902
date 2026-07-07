package io.github.panda17tk.arpg.save

import com.badlogic.gdx.Gdx
import io.github.panda17tk.arpg.sim.PlanetMemoryBook

/**
 * Where the universe's memory lives between runs (LP v2.28). RunSession takes this by injection —
 * null disables persistence entirely (the pre-v2.28 behaviour), the in-memory fake serves tests
 * and headless environments, and the Preferences store is the real device backend.
 */
interface MemoryStore {
    /** The persisted (spaceSeed, memory book), or null when nothing (readable) is stored. */
    fun load(): Pair<Long, PlanetMemoryBook>?
    fun save(spaceSeed: Long, book: PlanetMemoryBook)
    fun clear()
}

/** Test/headless double: holds the JSON in a field, exercising the same codec path as the real store. */
class InMemoryMemoryStore : MemoryStore {
    var stored: String? = null
    override fun load(): Pair<Long, PlanetMemoryBook>? = stored?.let { PlanetMemoryCodec.fromJson(it) }
    override fun save(spaceSeed: Long, book: PlanetMemoryBook) { stored = PlanetMemoryCodec.toJson(book, spaceSeed) }
    override fun clear() { stored = null }
}

/**
 * Gdx Preferences backend — one JSON string under [KEY] (a few KB for a whole star system).
 * Fully defensive like Scores: no backend or corrupt JSON never throws; it just reads as empty.
 */
class PreferencesMemoryStore(slot: Int = 0) : MemoryStore { // v2.103: one universe per slot
    private val key = SaveSlots.keyFor(KEY, slot)

    override fun load(): Pair<Long, PlanetMemoryBook>? = try {
        val text = Gdx.app.getPreferences(PREFS).getString(key, "")
        if (text.isNullOrEmpty()) null else PlanetMemoryCodec.fromJson(text)
    } catch (_: Throwable) { null }

    override fun save(spaceSeed: Long, book: PlanetMemoryBook) {
        try {
            val p = Gdx.app.getPreferences(PREFS)
            p.putString(key, PlanetMemoryCodec.toJson(book, spaceSeed))
            p.flush()
        } catch (_: Throwable) { /* persist best-effort */ }
    }

    override fun clear() {
        try {
            val p = Gdx.app.getPreferences(PREFS)
            p.remove(key)
            p.flush()
        } catch (_: Throwable) { /* best-effort */ }
    }

    private companion object {
        const val PREFS = "arpg-universe"
        const val KEY = "book.v1"
    }
}
