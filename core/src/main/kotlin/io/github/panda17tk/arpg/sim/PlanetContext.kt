package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome

/** The mood of a planet's society — how it greets a stranger and reacts to harm. */
enum class PlanetTemperament { GENTLE, PROUD, FEARFUL, HUNGRY, RITUALISTIC, VENGEFUL, ANCIENT, SILENT }

/** What a planet holds sacred — harming/taking it cuts deeper than anything else. */
enum class SacredThing { KING, CHILDREN, APEX, NEST, RELIC, FIRE, ICE, STORM, RUINS, SILENCE }

/** A one-line premise that flavours a planet's surface ecology (consumed by SurfaceEcology in a later loop). */
enum class PlanetStorySeed {
    NONE, HUNGRY_FOREST, LOST_CHILD, APEX_WORSHIP, PREDATOR_INVASION,
    BROKEN_SHRINE, EXILED_HEIR, SILENT_MONASTERY, NESTING_SEASON, RELIC_FAMINE,
}

/**
 * A planet's character: its [temperament], what it holds [sacredThing], and a [storySeed] that flavours its
 * surface. Generated deterministically from the planet's stable id + biome, so a given planet always has the
 * same soul. 惑星はステージではない。気質と信仰を持つ世界である。 Pure (no libGDX/Fleks) → unit-testable.
 */
data class PlanetContext(
    val temperament: PlanetTemperament,
    val sacredThing: SacredThing,
    val storySeed: PlanetStorySeed,
) {
    companion object {
        val NEUTRAL = PlanetContext(PlanetTemperament.SILENT, SacredThing.SILENCE, PlanetStorySeed.NONE)

        private const val SALT = 0x5A17C0FFEEL // keep this stream distinct from other id-keyed randomness

        /** Deterministic context for a planet, biased by [biome] and seeded by its stable [id]. */
        fun contextFor(id: Long, biome: PlanetBiome): PlanetContext {
            val rng = Rng(id xor SALT)
            return PlanetContext(
                pick(rng, temperaments(biome)),
                pick(rng, sacredThings(biome)),
                pick(rng, storySeeds(biome)),
            )
        }

        private fun <T> pick(rng: Rng, options: List<T>): T = options[rng.nextInt(options.size)]

        fun temperaments(biome: PlanetBiome): List<PlanetTemperament> = when (biome) {
            PlanetBiome.NATURE -> listOf(PlanetTemperament.GENTLE, PlanetTemperament.HUNGRY, PlanetTemperament.ANCIENT)
            PlanetBiome.MAGMA -> listOf(PlanetTemperament.PROUD, PlanetTemperament.RITUALISTIC, PlanetTemperament.VENGEFUL)
            PlanetBiome.ICE -> listOf(PlanetTemperament.PROUD, PlanetTemperament.FEARFUL, PlanetTemperament.ANCIENT)
            PlanetBiome.GAS -> listOf(PlanetTemperament.SILENT, PlanetTemperament.RITUALISTIC, PlanetTemperament.ANCIENT)
            PlanetBiome.DEAD -> listOf(PlanetTemperament.SILENT, PlanetTemperament.ANCIENT, PlanetTemperament.RITUALISTIC)
            PlanetBiome.LONELY -> listOf(PlanetTemperament.SILENT, PlanetTemperament.FEARFUL, PlanetTemperament.ANCIENT)
        }

        fun sacredThings(biome: PlanetBiome): List<SacredThing> = when (biome) {
            PlanetBiome.NATURE -> listOf(SacredThing.CHILDREN, SacredThing.APEX, SacredThing.NEST)
            PlanetBiome.MAGMA -> listOf(SacredThing.FIRE, SacredThing.KING, SacredThing.RELIC)
            PlanetBiome.ICE -> listOf(SacredThing.ICE, SacredThing.KING, SacredThing.CHILDREN)
            PlanetBiome.GAS -> listOf(SacredThing.STORM, SacredThing.RELIC)
            PlanetBiome.DEAD -> listOf(SacredThing.RUINS, SacredThing.SILENCE, SacredThing.RELIC)
            PlanetBiome.LONELY -> listOf(SacredThing.SILENCE, SacredThing.RELIC)
        }

        fun storySeeds(biome: PlanetBiome): List<PlanetStorySeed> = when (biome) {
            PlanetBiome.NATURE -> listOf(
                PlanetStorySeed.NONE, PlanetStorySeed.HUNGRY_FOREST, PlanetStorySeed.LOST_CHILD,
                PlanetStorySeed.NESTING_SEASON, PlanetStorySeed.PREDATOR_INVASION,
            )
            PlanetBiome.MAGMA -> listOf(
                PlanetStorySeed.NONE, PlanetStorySeed.APEX_WORSHIP, PlanetStorySeed.EXILED_HEIR, PlanetStorySeed.RELIC_FAMINE,
            )
            PlanetBiome.ICE -> listOf(
                PlanetStorySeed.NONE, PlanetStorySeed.EXILED_HEIR, PlanetStorySeed.LOST_CHILD, PlanetStorySeed.PREDATOR_INVASION,
            )
            PlanetBiome.GAS -> listOf(PlanetStorySeed.NONE, PlanetStorySeed.SILENT_MONASTERY)
            PlanetBiome.DEAD -> listOf(
                PlanetStorySeed.NONE, PlanetStorySeed.BROKEN_SHRINE, PlanetStorySeed.SILENT_MONASTERY, PlanetStorySeed.RELIC_FAMINE,
            )
            PlanetBiome.LONELY -> listOf(PlanetStorySeed.NONE, PlanetStorySeed.SILENT_MONASTERY, PlanetStorySeed.BROKEN_SHRINE)
        }
    }
}
