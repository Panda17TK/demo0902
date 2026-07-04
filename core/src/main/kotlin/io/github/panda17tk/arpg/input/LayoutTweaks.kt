package io.github.panda17tk.arpg.input

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * v2.56 ボタン配置エディタ: one button's user override — its centre as SCREEN FRACTIONS (so a
 * layout survives device changes and rotations) plus a size multiplier. Pure data + codec.
 */
data class ButtonTweak(val fx: Float, val fy: Float, val scale: Float = 1f)

object LayoutTweaks {
    const val SCALE_MIN = 0.6f
    const val SCALE_MAX = 1.6f

    /** Clamp a raw tweak into the legal envelope (fractions on screen, sane scale). */
    fun sanitize(t: ButtonTweak): ButtonTweak = ButtonTweak(
        fx = t.fx.coerceIn(0f, 1f),
        fy = t.fy.coerceIn(0f, 1f),
        scale = t.scale.coerceIn(SCALE_MIN, SCALE_MAX),
    )

    @Serializable
    private data class Dto(val name: String = "", val fx: Float = 0.5f, val fy: Float = 0.5f, val scale: Float = 1f)

    @Serializable
    private data class Book(val items: List<Dto> = emptyList())

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; isLenient = true }

    fun toJson(tweaks: Map<TouchButton, ButtonTweak>): String =
        json.encodeToString(Book.serializer(), Book(tweaks.map { (b, t) -> Dto(b.name, t.fx, t.fy, t.scale) }))

    /** Defensive parse: broken JSON → empty; unknown button names are skipped; values sanitized. */
    fun fromJson(text: String): Map<TouchButton, ButtonTweak> = try {
        json.decodeFromString(Book.serializer(), text).items.mapNotNull { d ->
            TouchButton.entries.firstOrNull { it.name == d.name }?.let { it to sanitize(ButtonTweak(d.fx, d.fy, d.scale)) }
        }.toMap()
    } catch (_: Throwable) {
        emptyMap()
    }
}
