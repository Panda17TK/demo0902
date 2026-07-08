package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.config.WildState

/**
 * Pure behaviour brain for a wild animal — given what it senses this tick, pick its [WildState].
 * No libGDX/Fleks, so it is fully unit-testable. WildlifeSystem does the sensing (neighbour scans,
 * herd centroid, nest threat) and the moving; this only decides intent.
 *
 * The watchword: 野生動物は生態系を作る — animals flee, herd, hunt and hold territory rather than
 * mindlessly charging the player.
 */
object WildAI {
    const val FLEE_DIST = 170f       // prey/herd bolt when the player gets this close
    const val TERRITORY_DIST = 300f  // apex / nest-guard notice an intruder within this
    const val STRIKE_DIST = 120f     // an apex closes to a chase inside this
    const val HUNGRY = 0.5f          // hunger ≥ this turns a predator from wandering to hunting
    const val WOUNDED = 0.3f         // below this hp fraction a prey/predator breaks and flees
    const val TIMID = 0.4f           // fear ≥ this means a nearby player alone is enough to spook it

    fun nextState(
        role: WildRole,
        hpFrac: Float,
        playerDist: Float,
        predatorNear: Boolean,
        preyNear: Boolean,
        nestThreatened: Boolean,
        herdSeparated: Boolean,
        hunger: Float,
        fear: Float,
        fleeSuppressed: Boolean = false, // LP v2.27: a grateful world's herds/hatchlings ignore the player
    ): WildState {
        val playerClose = playerDist < FLEE_DIST
        // A predator nearby always spooks; the player only spooks the timid.
        val spooked = predatorNear || (playerClose && fear >= TIMID)
        // Gratitude calms herds and hatchlings toward the PLAYER only — a predator still spooks them.
        val calmSpooked = predatorNear || (playerClose && fear >= TIMID && !fleeSuppressed)
        return when (role) {
            WildRole.HATCHLING ->
                if (predatorNear || (playerClose && !fleeSuppressed)) WildState.Flee else WildState.Wander

            WildRole.PREY ->
                if (spooked || hpFrac < WOUNDED) WildState.Flee else WildState.Graze

            WildRole.HERD -> when {
                calmSpooked || hpFrac < WOUNDED -> WildState.Flee
                herdSeparated -> WildState.Herd
                else -> WildState.Graze
            }

            // v2.131: SCHOOL never reaches this table (SchoolFishSystem owns the boids) —
            // if it ever does, fleeing when spooked is the only honest answer for a fish.
            WildRole.SCHOOL -> if (spooked) WildState.Flee else WildState.Wander

            WildRole.SWARM -> when {
                spooked -> WildState.Flee
                herdSeparated -> WildState.Herd
                else -> WildState.Wander
            }

            WildRole.SCAVENGER -> when {
                spooked -> WildState.Flee
                preyNear -> WildState.Feed // carrion / leftovers nearby
                else -> WildState.Wander
            }

            WildRole.PREDATOR -> when {
                hpFrac < WOUNDED -> WildState.Flee
                preyNear && hunger >= HUNGRY -> WildState.Chase
                preyNear -> WildState.Stalk
                playerClose -> WildState.Threaten // wary of the player rather than charging it
                hunger >= HUNGRY -> WildState.Hunt // prowl outward looking for prey
                else -> WildState.Wander
            }

            WildRole.APEX -> when {
                playerDist < STRIKE_DIST -> WildState.Chase
                playerDist < TERRITORY_DIST -> WildState.Threaten // an intruder in the territory
                preyNear && hunger >= HUNGRY -> WildState.Chase
                else -> WildState.Wander
            }

            WildRole.NEST_GUARD -> when {
                nestThreatened -> WildState.GuardNest
                playerDist < TERRITORY_DIST -> WildState.Threaten
                herdSeparated -> WildState.ReturnNest
                else -> WildState.Graze
            }

            WildRole.NONE -> WildState.Wander
        }
    }
}
