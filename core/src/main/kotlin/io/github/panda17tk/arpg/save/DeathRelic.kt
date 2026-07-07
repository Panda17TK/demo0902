package io.github.panda17tk.arpg.save

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * v2.46 遺品回収: what a death leaves floating where you fell — half the dust (rounded down)
 * and every gate shard. The NEXT run finds the bundle at the same spot (a fresh run always
 * rebuilds the same first system, so the coordinates stay meaningful); a surface death washes
 * the relic up near the space spawn instead.
 */
@Serializable
data class RelicDto(
    val x: Float = 0f, val y: Float = 0f,
    val dust: Int = 0, val shards: Int = 0,
    val space: Boolean = true, // false = died on a surface; spawn near the space start instead
)

object DeathRelic {
    /** The relic a death at (x,y) leaves, or null when there is nothing worth coming back for. */
    fun of(x: Float, y: Float, dust: Int, shards: Int, space: Boolean): RelicDto? {
        val d = dust / 2
        return if (d <= 0 && shards <= 0) null else RelicDto(x, y, d, shards, space)
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; isLenient = true }
    fun toJson(dto: RelicDto): String = json.encodeToString(RelicDto.serializer(), dto)
    fun fromJson(text: String): RelicDto? = try {
        json.decodeFromString(RelicDto.serializer(), text)
    } catch (_: Throwable) { null }
}

/** Same store register as RunSaveStore: an interface, an in-memory fake, a Preferences backend. */
interface RelicStore {
    fun load(): RelicDto?
    fun save(dto: RelicDto)
    fun clear()
}

class InMemoryRelicStore : RelicStore {
    var stored: String? = null
    override fun load(): RelicDto? = stored?.let { DeathRelic.fromJson(it) }
    override fun save(dto: RelicDto) { stored = DeathRelic.toJson(dto) }
    override fun clear() { stored = null }
}

class PreferencesRelicStore(slot: Int = 0) : RelicStore { // v2.103: each journey's own relic
    private val key = SaveSlots.keyFor(KEY, slot)

    override fun load(): RelicDto? = try {
        val text = com.badlogic.gdx.Gdx.app.getPreferences(PREFS).getString(key, "")
        if (text.isNullOrEmpty()) null else DeathRelic.fromJson(text)
    } catch (_: Throwable) { null }

    override fun save(dto: RelicDto) {
        try {
            val p = com.badlogic.gdx.Gdx.app.getPreferences(PREFS)
            p.putString(key, DeathRelic.toJson(dto))
            p.flush()
        } catch (_: Throwable) { /* persist best-effort */ }
    }

    override fun clear() {
        try {
            val p = com.badlogic.gdx.Gdx.app.getPreferences(PREFS)
            p.remove(key)
            p.flush()
        } catch (_: Throwable) { /* best-effort */ }
    }

    private companion object {
        const val PREFS = "arpg-relic"
        const val KEY = "relic.v1"
    }
}
