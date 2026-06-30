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

    override fun onTick() {
        if (worldState.mode != WorldMode.SPACE) { worldState.landingCandidate = null; return }
        with(world) {
            var px = 0f; var py = 0f; var has = false
            players.forEach { if (!has) { val t = it[Transform]; px = t.x; py = t.y; has = true } }
            worldState.landingCandidate = if (has) Landing.nearestLandable(px, py, planetField.planets, LAND_RANGE) else null
        }
    }

    companion object { private val LAND_RANGE = Tuning.TILE * 3f } // hover within ~3 tiles of the surface to land
}
