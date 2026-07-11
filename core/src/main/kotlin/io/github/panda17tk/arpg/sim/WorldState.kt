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
    /** v2.78 装飾: the surface's harmless furniture (trees/grass/rocks…) — set by WorldFactory. */
    var decor: List<io.github.panda17tk.arpg.map.Decor> = emptyList(),
    /** v2.79 水域: this surface's lakes and rivers — wading slows; frozen ponds just glitter. */
    var water: io.github.panda17tk.arpg.map.WaterBodies = io.github.panda17tk.arpg.map.WaterBodies.NONE,
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
    var trait: SystemTrait = SystemTrait.NONE, // v2.91 星系の個性 (SPACE only; surfaces stay calm)
    /** In SPACE, the system's jump gate (v2.44) — 3 key shards open the way to the next star system. */
    var gate: Pair<Float, Float>? = null,
    /** v2.87 儀式: display-only — true while the keeper holds enough shards; the gate lights up. */
    var gateReady: Boolean = false,
    /** v2.93 管制核: set by the screen when the sync tops out — the ending waits here. */
    var controlCore: Pair<Float, Float>? = null,
    /** v2.95 地下遺構: the sealed chamber's centre (SURFACE only) + first-entry latch. */
    var vault: Pair<Float, Float>? = null,
    var vaultEntered: Boolean = false,
    /** v2.45 星の依頼: this visit's kill tallies (any hostile / elite tier) — reset with the WorldState. */
    var questKills: Int = 0,
    var questElites: Int = 0,
    /** v2.68 星の依頼: dust picked up this visit + whether the memory core was stood before.
     *  coreVisited is independent of coreLogShown so the quest works with 世界観ヒント off. */
    var questDust: Int = 0,
    var coreVisited: Boolean = false,
    var coreArmed: Boolean = true, // v2.155: leaving the core re-arms the visit — no parked re-settles
    /** v2.69 星の依頼: predators put down this visit + seconds spent on the surface. */
    var questPredators: Int = 0,
    var questTime: Float = 0f,
    /** v2.72 連鎖: which request the star is on (0-based; CHAIN = all done) + the tallies'
     *  snapshot taken when the previous request settled — stage progress counts from here. */
    var questStage: Int = 0,
    var questBaseKills: Int = 0,
    var questBaseElites: Int = 0,
    var questBaseDust: Int = 0,
    var questBasePredators: Int = 0,
    var questBaseTime: Float = 0f,
    // v2.150 記録の清潔: 依頼の連鎖の段の継ぎ目 — 全カウンタの基準を取り直し、CORE の
    // 訪問印も下ろす（連鎖内で CORE が2度出ても、2度目が即時決済されない）。
    /** v2.46 難破船: wrecked hulls adrift in the system — guarded loot caches worth boarding. */
    var wrecks: List<Pair<Float, Float>> = emptyList(),
    /** v2.100 行商船: a friendly trading vessel adrift in SOME systems (null when it isn't here). */
    var trader: Pair<Float, Float>? = null,
    /** v2.110 行商船襲撃: 0=まだ / 1=進行中 / 2=決着。守り抜けば the shop discounts 20%. */
    var traderRaid: Int = 0,
    var traderRescued: Boolean = false,
    /** v2.110 生存者: the wreck index sheltering a survivor (-1 = none) + the rescue latch. */
    var survivorWreck: Int = -1,
    var survivorRescued: Boolean = false,
    /** v2.110 彗星: the head + tail direction (SPACE, some skies) — dust beads ride the tail. */
    var comet: Pair<Float, Float>? = null,
    var cometDir: Pair<Float, Float>? = null,
    /** v2.48 惑星サーバー: the surface's memory core (Layer 1) — stand before it and it speaks once. */
    var memoryCore: Pair<Float, Float>? = null,
    var coreLogShown: Boolean = false,
    /** v2.51: wreck indices whose distress log has already been broadcast to this visitor. */
    val wreckLogShown: MutableSet<Int> = mutableSetOf(),
    /** v2.75 天候: this landing's sky — set once by WorldFactory; ecology/render/sound all read it. */
    var weather: WeatherKind = WeatherKind.CLEAR,
) {
    fun snapshotQuestBases() {
        questBaseKills = questKills; questBaseElites = questElites
        questBaseDust = questDust; questBasePredators = questPredators
        questBaseTime = questTime
        coreVisited = false
    }
}
