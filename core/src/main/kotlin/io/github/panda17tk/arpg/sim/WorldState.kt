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
    /** On a SURFACE, what the society remembers of this visit (ecology events, player deeds). Reset per landing. */
    var society: PlanetSocietyState = PlanetSocietyState(),
    /** In SPACE, the drifting debris/asteroid field flowing around the player (cosmetic; null on a surface). */
    var drift: DriftField? = null,
    /** On a SURFACE, the landed planet's character (temperament/sacred/story) — drives ecology, AI, speech. */
    var context: PlanetContext? = null,
    /** On a SURFACE just landed on a remembered planet, true briefly so the HUD greets the player by reputation. */
    var rememberedPlanet: Boolean = false,
    /** On a SURFACE, the return-visit greeting this planet's memory warrants (null on a first visit / faint memory). */
    var returnVisitGreeting: SocietySpeechTrigger? = null,
    /** On a SURFACE, the live event feed (LP v2.24) — filled/aged by EventFeedSystem, drawn by Hud.eventFeed.
     *  Lives here so every transition resets it for free (the WorldState is rebuilt per landing). */
    val recentEvents: MutableList<PlanetEvent> = mutableListOf(),
    /** On a SURFACE, how the planet's memory reshaped this landing's spawn (LP v2.27) — set once by WorldFactory. */
    var spawnTweaks: SpawnTweaks = SpawnTweaks.NEUTRAL,
    /** In SPACE, the system's jump gate (v2.44) — 3 key shards open the way to the next star system. */
    var gate: Pair<Float, Float>? = null,
    /** v2.45 星の依頼: this visit's kill tallies (any hostile / elite tier) — reset with the WorldState. */
    var questKills: Int = 0,
    var questElites: Int = 0,
    /** v2.68 星の依頼: dust picked up this visit + whether the memory core was stood before.
     *  coreVisited is independent of coreLogShown so the quest works with 世界観ヒント off. */
    var questDust: Int = 0,
    var coreVisited: Boolean = false,
    /** v2.69 星の依頼: predators put down this visit + seconds spent on the surface. */
    var questPredators: Int = 0,
    var questTime: Float = 0f,
    /** v2.46 難破船: wrecked hulls adrift in the system — guarded loot caches worth boarding. */
    var wrecks: List<Pair<Float, Float>> = emptyList(),
    /** v2.48 惑星サーバー: the surface's memory core (Layer 1) — stand before it and it speaks once. */
    var memoryCore: Pair<Float, Float>? = null,
    var coreLogShown: Boolean = false,
    /** v2.51: wreck indices whose distress log has already been broadcast to this visitor. */
    val wreckLogShown: MutableSet<Int> = mutableSetOf(),
)
