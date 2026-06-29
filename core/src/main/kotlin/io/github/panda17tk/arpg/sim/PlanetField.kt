package io.github.panda17tk.arpg.sim

/** Injected holder of the stage's discrete planets (world coords), shared by gravity, collision and rendering. */
class PlanetField(var planets: List<PlanetBody> = emptyList())
