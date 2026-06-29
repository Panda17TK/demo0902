package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome

/** Whether play is happening in open space or down on a planet's surface (Living Planets LP-E). */
enum class WorldMode { SPACE, SURFACE }

/** Injected holder for the current world mode + (in SPACE) the planet the player is hovering to land on. */
class WorldState(
    var mode: WorldMode = WorldMode.SPACE,
    var biome: PlanetBiome? = null,
    var landingCandidate: PlanetBody? = null,
    /** On a SURFACE, the world-space escape pad (the landing point) the player returns to to take off. */
    var escapePad: Pair<Float, Float>? = null,
    /** On a SURFACE, the built landmarks (camp/crater/dais/eye/shrine/ruins) the society lives among. */
    var facilities: List<Facility> = emptyList(),
)
