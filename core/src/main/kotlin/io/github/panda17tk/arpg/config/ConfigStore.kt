package io.github.panda17tk.arpg.config

import com.badlogic.gdx.Gdx

/**
 * Holds the active [GameConfig]. In-memory ops (import/export/reset) are pure and tested;
 * disk load/save use Gdx.files (only available inside a running libGDX app).
 */
class ConfigStore {
    var config: GameConfig = GameConfig()
        private set

    /** Replace the active config from a JSON string (partial JSON fills from defaults). */
    fun import(json: String) { config = ConfigCodec.fromJson(json) }

    /** Serialize the active config to JSON. */
    fun export(): String = ConfigCodec.toJson(config)

    /** Back to built-in defaults. */
    fun reset() { config = GameConfig() }

    /** Mutate in place (used by the dev-editor in Phase 8). */
    fun update(newConfig: GameConfig) { config = newConfig }

    // ---- disk I/O (Gdx-dependent; not unit-tested) ----
    private val fileName = "config.json"

    /** Load an override file from local storage if present; otherwise keep defaults. */
    fun loadFromDisk() {
        val handle = Gdx.files.local(fileName)
        if (handle.exists()) {
            runCatching { import(handle.readString("UTF-8")) }
                .onFailure { Gdx.app.error("ConfigStore", "bad config.json, using defaults", it) }
        }
    }

    /** Persist the active config to local storage. */
    fun saveToDisk() {
        Gdx.files.local(fileName).writeString(export(), false, "UTF-8")
    }
}
