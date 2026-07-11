package io.github.panda17tk.arpg.save

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The serialized form of one saved run (v2.33 インベントリのセーブ): the world's identity (mode/
 * biome/seeds) plus the player's vitals, pools, upgrades and gear. A plain DTO, never the domain
 * types (§11); loadout slots map slot-name → item id, the backpack is a list of item ids.
 */
@Serializable
data class RunSaveDto(
    val version: Int = 1,
    // Which world to rebuild
    val mode: String = "SPACE",          // WorldMode name
    val biome: String? = null,           // PlanetBiome name (surface runs)
    val spaceSeed: Long = 1L,
    val surfSeed: Long = 100L,
    val worldSeed: Long = 1L,            // the seed the CURRENT world was built with
    val landedPlanetId: Long? = null,
    val returnX: Float? = null, val returnY: Float? = null,
    val wave: Int = 1,
    val areaX: Int = 1, val areaY: Int = 1, // v2.166 宙域の九分割 (append-only; old saves read centre)
    // v2.169 診断修正: the sky's one-time treasures (append-only; old saves read them fresh)
    val lootedWrecks: List<Int> = emptyList(),
    val survivorRescued: Boolean = false,
    val cometSwept: Boolean = false,
    // Player vitals + position
    val px: Float = 0f, val py: Float = 0f,
    val hp: Float = 100f, val hpMax: Float = 100f,
    val stamina: Float = 100f,
    // Pools + upgrades
    val ammo9: Int = 0, val ammo12: Int = 0, val ammoBeam: Int = 0, val ammoNade: Int = 0,
    val blocks: Int = 0,
    val dust: Int = 0, // 星屑 (v2.43)
    val shards: Int = 0, // ゲート鍵の断片 (v2.44)
    val mags: List<Int> = emptyList(),
    val gunMul: Float = 1f, val fireMul: Float = 1f, val meleeMul: Float = 1f, val moveMul: Float = 1f,
    val ammoMul: Float = 1f, val healOnKill: Float = 0f, val wallHp: Float = 70f, // v2.151: matches Mods.wallHp — a save missing the key restores the real default
    // v2.107 強化カード拡充 (append-only; old saves read the neutral defaults)
    val reloadMul: Float = 1f, val blastMul2: Float = 1f, val regenAdd: Float = 0f,
    val dashCostMul: Float = 1f, val bulletSpeedMul: Float = 1f,
    val armorMul: Float = 1f, val pickupRange: Float = 0f,
    // Gear (v2.33): slot name → item id; backpack as ids
    val loadout: Map<String, String> = emptyMap(),
    val backpack: List<String> = emptyList(),
    val curWeapon: Int = 0,
    // Surface runs only: this visit's society snapshot (the planet's context is recomputed
    // deterministically from landedPlanetId + biome, so it needs no field of its own).
    val society: PlanetMemoryDto? = null,
)

/** Pure RunSaveDto <-> JSON, defensive like PlanetMemoryCodec: broken JSON reads as null. */
object RunSaveCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    fun toJson(dto: RunSaveDto): String = json.encodeToString(RunSaveDto.serializer(), dto)

    fun fromJson(text: String): RunSaveDto? = try {
        json.decodeFromString(RunSaveDto.serializer(), text)
    } catch (_: Throwable) {
        null // corrupt/alien JSON → no save; the caller starts a fresh run
    }
}

/**
 * Where a saved run lives (v2.33). Same register as MemoryStore: an interface RunSession-level
 * code depends on, an in-memory fake for tests, and a Preferences backend for the device.
 */
interface RunSaveStore {
    fun load(): RunSaveDto?
    fun save(dto: RunSaveDto)
    fun clear()
}

/** Test/headless double: holds the JSON in a field, exercising the same codec path as the real store. */
class InMemoryRunSaveStore : RunSaveStore {
    var stored: String? = null
    override fun load(): RunSaveDto? = stored?.let { RunSaveCodec.fromJson(it) }
    override fun save(dto: RunSaveDto) { stored = RunSaveCodec.toJson(dto) }
    override fun clear() { stored = null }
}

/** Gdx Preferences backend — one JSON string per slot (v2.103; slot 0 keeps the legacy key). */
class PreferencesRunSaveStore(slot: Int = 0) : RunSaveStore {
    private val key = SaveSlots.keyFor(KEY, slot)

    override fun load(): RunSaveDto? = try {
        val text = com.badlogic.gdx.Gdx.app.getPreferences(PREFS).getString(key, "")
        if (text.isNullOrEmpty()) null else RunSaveCodec.fromJson(text)
    } catch (_: Throwable) { null }

    override fun save(dto: RunSaveDto) {
        try {
            val p = com.badlogic.gdx.Gdx.app.getPreferences(PREFS)
            p.putString(key, RunSaveCodec.toJson(dto))
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
        const val PREFS = "arpg-run"
        const val KEY = "run.v1"
    }
}
