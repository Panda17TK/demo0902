package io.github.panda17tk.arpg.config

import kotlinx.serialization.json.Json

/** Pure GameConfig <-> JSON. Lenient so partial/edited files fill from defaults. */
object ConfigCodec {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    fun toJson(config: GameConfig): String = json.encodeToString(GameConfig.serializer(), config)
    fun fromJson(text: String): GameConfig = json.decodeFromString(GameConfig.serializer(), text)
}
