package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome

/**
 * 1ランぶんの星系状態と惑星記憶 (LP R1): the star system's seed, the per-landing surface seed, the
 * per-planet memory book, and where to re-emerge after a takeoff. GameScreen executes transitions;
 * the decisions live here so they are unit-testable and so persistence (v2.28) has one owner to hook.
 * Pure (no libGDX/Fleks).
 */
class RunSession(
    var spaceSeed: Long = 1L,   // the current star system's seed (stable, so round-trips return to it)
    var surfSeed: Long = 100L,  // surface seed, varied per landing
    val memory: PlanetMemoryBook = PlanetMemoryBook(), // per-planet society memory, survives landings this run
    var landedPlanetId: Long? = null,                  // id of the planet we're on, so takeoff folds memory back in
    var returnSpawn: Pair<Float, Float>? = null,       // where to re-emerge in space after taking off
) {
    /** 着陸1回ぶんの決定事項 — everything GameScreen needs to build the surface and greet the player. */
    data class LandingPlan(
        val seed: Long,
        val biome: PlanetBiome,
        val context: PlanetContext,
        val society: PlanetSocietyState,     // recall()ed copy that seeds this visit
        val known: Boolean,                  // the planet has been visited before
        val greeting: SocietySpeechTrigger?, // landing greeting (known planets only)
        val showGreeting: Boolean,           // ReturnVisitLine has a HUD sentence for it
    )

    /** Decide one landing: advance the surface seed, remember the return spot, recall the planet's memory. */
    fun planLanding(cand: PlanetBody): LandingPlan {
        returnSpawn = ReturnSpawn.beside(cand) // remember where to re-emerge on takeoff
        surfSeed += 1
        landedPlanetId = cand.id
        val known = memory.knows(cand.id)
        val recalled = memory.recall(cand.id)
        return LandingPlan(
            seed = surfSeed,
            biome = cand.biome,
            context = cand.context,
            society = recalled,
            known = known,
            greeting = if (known) SocietySpeechLines.returnGreeting(recalled) else null,
            showGreeting = known && ReturnVisitLine.hudLine(recalled) != null,
        )
    }

    /** Fold the finished visit back into memory; returns (spaceSeed, returnSpawn) for the space rebuild. */
    fun completeTakeoff(surface: PlanetSocietyState): Pair<Long, Pair<Float, Float>?> {
        landedPlanetId?.let { memory.remember(it, surface) }
        landedPlanetId = null
        return spaceSeed to returnSpawn
    }

    /** A fresh run: reset the seeds and forget every planet (persistence will replace this in v2.28). */
    fun reset() {
        spaceSeed = 1L
        surfSeed = 100L
        landedPlanetId = null
        returnSpawn = null
        memory.memories.clear()
    }
}
