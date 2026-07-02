package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.save.MemoryStore

/**
 * 1ランぶんの星系状態と惑星記憶 (LP R1): the star system's seed, the per-landing surface seed, the
 * per-planet memory book, and where to re-emerge after a takeoff. GameScreen executes transitions;
 * the decisions live here so they are unit-testable. With a [store] injected (LP v2.28) the fixed
 * spaceSeed=1 universe persists across runs and app restarts — 星はあなたを覚えている; null keeps
 * the pre-persistence behaviour (tests / opt-out). Pure logic (the store interface carries no libGDX).
 */
class RunSession(
    var spaceSeed: Long = 1L,   // the current star system's seed (stable, so round-trips return to it)
    var surfSeed: Long = 100L,  // surface seed, varied per landing
    val memory: PlanetMemoryBook = PlanetMemoryBook(), // per-planet society memory, survives landings this run
    var landedPlanetId: Long? = null,                  // id of the planet we're on, so takeoff folds memory back in
    var returnSpawn: Pair<Float, Float>? = null,       // where to re-emerge in space after taking off
    private val store: MemoryStore? = null,            // persistence backend; null = memory dies with the run
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

    /** Fold the finished visit back into memory (and persist it); returns (spaceSeed, returnSpawn). */
    fun completeTakeoff(surface: PlanetSocietyState): Pair<Long, Pair<Float, Float>?> {
        landedPlanetId?.let { memory.remember(it, surface) }
        landedPlanetId = null
        persist() // FR-8.2 ①: a takeoff checkpoints the universe
        return spaceSeed to returnSpawn
    }

    /** Restore the persisted universe (call once the platform backend is ready). Wrong-seed saves are ignored. */
    fun restore() {
        val (seed, book) = store?.load() ?: return
        if (seed != spaceSeed) return
        memory.memories.clear()
        memory.memories.putAll(book.memories)
    }

    /** Persist the universe now (FR-8.2 ②: the game-over checkpoint). No store → no-op. */
    fun persist() { store?.save(spaceSeed, memory) }

    /** FR-8.3: 「宇宙の記憶を消す」 — every star forgets you, in memory and on disk. */
    fun forgetUniverse() {
        memory.memories.clear()
        store?.clear()
    }

    /**
     * A fresh run: reset the seeds. WITHOUT a store this also forgets every planet (the pre-persistence
     * behaviour); with one, the universe's memory survives the restart — 一度の罪は永遠 (FR-8.1).
     */
    fun reset() {
        spaceSeed = 1L
        surfSeed = 100L
        landedPlanetId = null
        returnSpawn = null
        if (store == null) memory.memories.clear()
    }
}
