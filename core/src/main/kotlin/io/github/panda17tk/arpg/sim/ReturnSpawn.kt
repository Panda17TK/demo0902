package io.github.panda17tk.arpg.sim

/** Pure: where the player re-emerges in space after taking off — just outside the planet they left. */
object ReturnSpawn {
    /**
     * A spot off the planet's surface (above it) — close enough that you return next to the world you left
     * and it stays in view, but far enough out that its gravity doesn't immediately yank you back down.
     */
    fun beside(planet: PlanetBody, margin: Float = 120f): Pair<Float, Float> =
        planet.cx to (planet.cy - planet.radius - margin)
}
