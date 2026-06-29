package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.map.Biome

/** Whether play is happening in open space or down on a planet's surface (Living Planets LP-E). */
enum class WorldMode { SPACE, SURFACE }

/** Injected holder for the current world mode + (in SPACE) the planet the player is hovering to land on. */
class WorldState(
    var mode: WorldMode = WorldMode.SPACE,
    var biome: Biome? = null,
    var landingCandidate: PlanetBody? = null,
)
