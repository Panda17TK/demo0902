package io.github.panda17tk.arpg.sim

/** Pure: where the player re-emerges in space after taking off — just outside the planet they left. */
object ReturnSpawn {
    /** A spot a short way off the planet's surface (above it), so you return next to the world you left. */
    fun beside(planet: PlanetBody, margin: Float = 40f): Pair<Float, Float> =
        planet.cx to (planet.cy - planet.radius - margin)
}
