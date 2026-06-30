package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.sim.Landing
import io.github.panda17tk.arpg.sim.PlanetField
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.sim.WorldMode
import io.github.panda17tk.arpg.sim.WorldState

/** Flags the planet the player is hovering over (SPACE mode only) so the HUD can offer landing. */
class LandingSystem : IntervalSystem() {
    private val planetField: PlanetField = world.inject()
    private val worldState: WorldState = world.inject()
    private val players by lazy { world.family { all(PlayerTag, Transform) } }
    private var graceTicks = 0 // keeps the last candidate "landable" briefly after you drift past it

    override fun onTick() {
        if (worldState.mode != WorldMode.SPACE) { worldState.landingCandidate = null; graceTicks = 0; return }
        with(world) {
            var px = 0f; var py = 0f; var has = false
            players.forEach { if (!has) { val t = it[Transform]; px = t.x; py = t.y; has = true } }
            val cand = if (has) Landing.nearestLandable(px, py, planetField.planets, LAND_RANGE) else null
            // Frictionless space drifts you past a planet fast; latch the candidate for a short grace window so
            // the touch LAND button stays visible+tappable (and a tap still has a planet to land on) for ~0.8s.
            when {
                cand != null -> { worldState.landingCandidate = cand; graceTicks = GRACE_TICKS }
                graceTicks > 0 -> graceTicks--
                else -> worldState.landingCandidate = null
            }
        }
    }

    companion object {
        private val LAND_RANGE = Tuning.TILE * 6f // hover within ~6 tiles of the surface to land (forgiving in vast space)
        private const val GRACE_TICKS = 48        // ~0.8s at 60Hz fixed step
    }
}
