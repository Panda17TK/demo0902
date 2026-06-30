package io.github.panda17tk.arpg.sim

/**
 * Persistent per-planet society memory, keyed by [PlanetBody.id]. 惑星はステージではない。記憶を持つ世界である。
 *
 * Lives on the transition owner (GameScreen), surviving the world rebuilds that happen on every landing/takeoff.
 * A landing seeds the surface [WorldState.society] from [recall]; a takeoff folds the visit back via [remember].
 * Pure data + two small folds — no libGDX/Fleks, so it is unit-testable.
 */
class PlanetMemoryBook(
    val memories: MutableMap<Long, PlanetSocietyState> = mutableMapOf(),
) {
    /** Whether this planet has ever been visited (and so carries remembered state). */
    fun knows(id: Long): Boolean = memories.containsKey(id)

    /** A fresh copy of a planet's remembered society to seed a surface visit (empty state if never visited). */
    fun recall(id: Long): PlanetSocietyState = memories[id]?.copyState() ?: PlanetSocietyState()

    /** Fold a finished surface visit's society back into the planet's persistent memory. */
    fun remember(id: Long, surface: PlanetSocietyState) {
        memories.getOrPut(id) { PlanetSocietyState() }.mergeFrom(surface)
    }
}
