package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.IntervalSystem
import io.github.panda17tk.arpg.sim.PlanetContext
import io.github.panda17tk.arpg.sim.PlanetSocietyState
import io.github.panda17tk.arpg.sim.SocietyEventDiff
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.sim.WorldMode
import io.github.panda17tk.arpg.sim.WorldState

/**
 * The surface event feed (LP v2.24): watches [WorldState.society] for the edges the emitters carve
 * into it and turns each into a short HUD line in [WorldState.recentEvents]. Read-only toward the
 * society — the emitters (EcologyEventSystem / PickupSystem / …) are untouched, so any future
 * emitter is picked up automatically, at most one tick late. The first surface tick only snapshots
 * the baseline, so a recalled (already-dirty) memory never dumps stale events on landing. Also owns
 * aging + expiry, which freeze with the sim while paused. Surface only.
 */
class EventFeedSystem : IntervalSystem() {
    private val worldState: WorldState = world.inject()
    private var prev: PlanetSocietyState? = null

    override fun onTick() {
        if (worldState.mode != WorldMode.SURFACE) return
        val ctx = worldState.context ?: PlanetContext.NEUTRAL
        val cur = worldState.society
        val events = worldState.recentEvents
        prev?.let { before ->
            events.addAll(SocietyEventDiff.diff(before, cur, ctx))
            while (events.size > Tuning.EVENT_FEED_MAX) events.removeAt(0) // oldest out first
        }
        prev = cur.copyState() // the one per-tick copy the perf budget allows (§14.2)
        // Age every line; drop the expired (iterate backwards so removal is safe + cheap).
        for (i in events.indices.reversed()) {
            val e = events[i]
            e.age += deltaTime
            if (e.age >= Tuning.EVENT_FEED_LIFE) events.removeAt(i)
        }
    }
}
