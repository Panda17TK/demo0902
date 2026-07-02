package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.FamilyRole
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.PlanetSocietyState
import io.github.panda17tk.arpg.sim.ReturnVisitEffects
import io.github.panda17tk.arpg.sim.Tuning
import io.github.panda17tk.arpg.sim.WorldMode
import kotlin.math.hypot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** LP v2.27: a remembered planet reshapes the landing's spawn (guards / thinning / hunger). */
class ReturnVisitSpawnTest {
    private fun surface(society: PlanetSocietyState?) = WorldFactory.create(
        InputState(), seed = 5L, mode = WorldMode.SURFACE, biome = PlanetBiome.NATURE, society = society,
    )

    @Test fun `a hostile memory posts a guardian watch beside the pad`() {
        val gw = surface(PlanetSocietyState(hostility = 0.7f))
        val pad = gw.worldState.escapePad!!
        var guardsNearPad = 0
        with(gw.world) {
            gw.world.family { all(Mob, Transform) }.forEach { e ->
                val m = e[Mob]; val t = e[Transform]
                if (m.def.familyRole == FamilyRole.GUARDIAN &&
                    hypot(t.x - pad.first, t.y - pad.second) <= Tuning.TILE * 6f) guardsNearPad++
            }
        }
        assertTrue(guardsNearPad >= 1, "expected a watch-guard near the pad, found $guardsNearPad")
        assertEquals(1, gw.worldState.spawnTweaks.extraGuardsAtPad)
    }

    @Test fun `a first visit spawns no watch and keeps neutral tweaks`() {
        val gw = surface(null)
        assertEquals(0, gw.worldState.spawnTweaks.extraGuardsAtPad)
        assertEquals(1f, gw.worldState.spawnTweaks.herbivoreMul)
    }

    @Test fun `a disrupted memory starts the hunters hungry`() {
        val gw = surface(PlanetSocietyState(ecologicalDisruption = 0.6f))
        var hungryHunters = 0
        with(gw.world) {
            gw.world.family { all(Mob) }.forEach { e ->
                val m = e[Mob]
                if (m.hunger >= ReturnVisitEffects.PREDATOR_HUNGER) hungryHunters++
            }
        }
        assertTrue(hungryHunters >= 1, "expected pre-hungered predators, found $hungryHunters")
    }
}
