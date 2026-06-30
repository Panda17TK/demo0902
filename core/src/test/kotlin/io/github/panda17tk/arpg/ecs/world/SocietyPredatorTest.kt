package io.github.panda17tk.arpg.ecs.world

import com.github.quillraven.fleks.Entity
import io.github.panda17tk.arpg.config.FamilyRole
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.ecs.components.CreatureMind
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.CreatureState
import io.github.panda17tk.arpg.sim.WorldMode
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The headline cross-layer consequence: a wild predator stalking a child rouses the society's guardian. */
class SocietyPredatorTest {
    private fun find(gw: GameWorld, pred: (Mob) -> Boolean): Entity? {
        var found: Entity? = null
        gw.world.family { all(Mob, Transform) }.forEach { e ->
            if (found == null && with(gw.world) { pred(e[Mob]) }) found = e
        }
        return found
    }

    @Test fun `a guardian defends a child stalked by a wild predator`() {
        val gw = WorldFactory.create(InputState(), seed = 3L, mode = WorldMode.SURFACE, biome = PlanetBiome.NATURE)
        val guardian = find(gw) { it.def.lifeKind != LifeKind.WILDLIFE && it.def.familyRole == FamilyRole.GUARDIAN }
        val child = find(gw) { it.def.familyRole == FamilyRole.CHILD }
        val predator = find(gw) { it.def.lifeKind == LifeKind.WILDLIFE && (it.def.wildRole == WildRole.PREDATOR || it.def.wildRole == WildRole.APEX) }
        assertNotNull(guardian, "nature should have a guardian")
        assertNotNull(child, "nature should have a child")
        assertNotNull(predator, "nature should have a predator")

        with(gw.world) {
            // Same tribe: the child reads as a ward, and the predator is never a hostile-tribe brawl target —
            // so the only thing rousing the guardian is the wild-predator-near-ward path under test.
            val gTribe = guardian!![Mob].tribe
            child!![Mob].tribe = gTribe
            predator!![Mob].tribe = gTribe
        }
        // Send the player far away so the reaction is driven by the predator, not by the player.
        with(gw.world) {
            gw.world.family { all(PlayerTag, Transform) }.forEach { p ->
                val pt = p[Transform]; pt.x = 30000f; pt.y = 30000f; pt.prevX = pt.x; pt.prevY = pt.y
            }
        }
        fun cluster() = with(gw.world) {
            val gt = guardian!![Transform]; val ct = child!![Transform]; val pt = predator!![Transform]
            ct.x = gt.x + 30f; ct.y = gt.y; ct.prevX = ct.x; ct.prevY = ct.y // child beside the guardian
            pt.x = gt.x + 50f; pt.y = gt.y; pt.prevX = pt.x; pt.prevY = pt.y // predator right on the child
        }
        var roused = false
        repeat(12) {
            cluster()
            gw.world.update(1f / 60f)
            val s = with(gw.world) { guardian!![CreatureMind].state }
            if (s == CreatureState.Protect || s == CreatureState.Rally) roused = true
        }
        assertTrue(roused, "the guardian should Protect/Rally when a wild predator stalks the child")
    }
}
