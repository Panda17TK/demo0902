package io.github.panda17tk.arpg.save

import io.github.panda17tk.arpg.sim.PlanetMemoryBook
import io.github.panda17tk.arpg.sim.PlanetSocietyState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The serialized form of one planet's remembered society — a plain DTO, never the domain type (§11). */
@Serializable
data class PlanetMemoryDto(
    val childHarmed: Boolean = false,
    val childKilled: Boolean = false,
    val wildPredatorThreatenedChild: Boolean = false,
    val predatorKilledNearChild: Boolean = false,
    val hatchlingKilled: Boolean = false,
    val nestMotherKilled: Boolean = false,
    val apexKilled: Boolean = false,
    val surrenderKilled: Int = 0,
    val surrenderedSpared: Int = 0,
    val leaderDefeated: Boolean = false,
    val relicClaimed: Boolean = false,
    val hostility: Float = 0f,
    val mercy: Float = 0f,
    val ecologicalDisruption: Float = 0f,
)

@Serializable
data class MemoryBookDto(
    val version: Int = 1,
    val spaceSeed: Long = 1L,
    val planets: Map<Long, PlanetMemoryDto> = emptyMap(),
)

/**
 * Pure PlanetMemoryBook <-> JSON (LP v2.28), following ConfigCodec's register: a @Serializable DTO
 * between the domain type and the wire, lenient parsing with unknown keys ignored and missing
 * fields defaulted, and a version field for future migration. Broken JSON reads as null — the
 * caller starts with an empty memory rather than crash.
 */
object PlanetMemoryCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    fun toJson(book: PlanetMemoryBook, spaceSeed: Long): String = json.encodeToString(
        MemoryBookDto.serializer(),
        MemoryBookDto(spaceSeed = spaceSeed, planets = book.memories.mapValues { (_, s) -> s.toDto() }),
    )

    fun fromJson(text: String): Pair<Long, PlanetMemoryBook>? = try {
        val dto = json.decodeFromString(MemoryBookDto.serializer(), text)
        val book = PlanetMemoryBook()
        for ((id, p) in dto.planets) book.memories[id] = p.toState()
        dto.spaceSeed to book
    } catch (_: Throwable) {
        null // corrupt/alien JSON → the caller starts from a blank memory
    }

    private fun PlanetSocietyState.toDto() = PlanetMemoryDto(
        childHarmed, childKilled, wildPredatorThreatenedChild, predatorKilledNearChild,
        hatchlingKilled, nestMotherKilled, apexKilled, surrenderKilled, surrenderedSpared,
        leaderDefeated, relicClaimed, hostility, mercy, ecologicalDisruption,
    )

    private fun PlanetMemoryDto.toState() = PlanetSocietyState(
        childHarmed, childKilled, wildPredatorThreatenedChild, predatorKilledNearChild,
        hatchlingKilled, nestMotherKilled, apexKilled, surrenderKilled, surrenderedSpared,
        leaderDefeated, relicClaimed, hostility, mercy, ecologicalDisruption,
    )
}
