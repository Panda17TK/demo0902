package io.github.panda17tk.arpg.sim

/** The colour family of a feed line — hostile red, merciful green, ecology amber, neutral ink. */
enum class EventKind { HOSTILE, MERCY, ECOLOGY, NEUTRAL }

/**
 * One line of the surface event feed (LP v2.24): the moment a deed lands in the society's memory,
 * shown to the player as a short sentence. [age] is advanced by EventFeedSystem; the HUD fades the
 * line out over its last moments and the system drops it past [Tuning.EVENT_FEED_LIFE].
 */
data class PlanetEvent(val text: String, val kind: EventKind, var age: Float = 0f)
