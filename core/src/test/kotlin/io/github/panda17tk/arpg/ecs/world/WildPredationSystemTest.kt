package io.github.panda17tk.arpg.ecs.world

import com.github.quillraven.fleks.Entity
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.WorldMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WildPredationSystemTest {
    private fun natureWorld() =
        WorldFactory.create(InputState(), seed = 3L, mode = WorldMode.SURFACE, biome = PlanetBiome.NATURE)

    private fun find(gw: GameWorld, pred: (Mob) -> Boolean): Entity? {
        var found: Entity? = null
        gw.world.family { all(Mob, Transform, Health) }.forEach { e ->
            if (found == null && with(gw.world) { pred(e[Mob]) }) found = e
        }
        return found
    }

    private fun predatorOf(gw: GameWorld) =
        find(gw) { it.def.lifeKind == LifeKind.WILDLIFE && (it.def.wildRole == WildRole.PREDATOR || it.def.wildRole == WildRole.APEX) }
    private fun preyOf(gw: GameWorld) =
        find(gw) { it.def.lifeKind == LifeKind.WILDLIFE && it.def.wildRole == WildRole.PREY }

    @Test fun `a wild predator bites adjacent prey and feeds`() {
        val gw = natureWorld()
        val predator = predatorOf(gw); val prey = preyOf(gw)
        assertNotNull(predator, "nature should have a wild predator")
        assertNotNull(prey, "nature should have wild prey")
        val hpBefore = with(gw.world) {
            val pt = predator!![Transform]; val pm = predator[Mob]
            pm.feedCd = 0f; pm.hunger = 0.8f
            val qt = prey!![Transform]
            qt.x = pt.x + 18f; qt.y = pt.y; qt.prevX = qt.x; qt.prevY = qt.y // inside BITE_RANGE (~40px)
            prey[Health].hp
        }
        gw.world.update(1f / 60f)
        with(gw.world) {
            assertTrue(prey!![Health].hp < hpBefore, "the predator should have bitten the prey: ${prey[Health].hp} vs $hpBefore")
            assertTrue(predator!![Mob].hunger < 0.8f, "feeding should reduce hunger, was ${predator[Mob].hunger}")
        }
    }

    @Test fun `the feed cooldown stops every-frame chewing`() {
        val gw = natureWorld()
        val predator = predatorOf(gw); val prey = preyOf(gw)
        assertNotNull(predator); assertNotNull(prey)
        // Shove every OTHER wild hunter far away so only our predator can reach the prey.
        gw.world.family { all(Mob, Transform) }.forEach { e ->
            if (e != predator && e != prey) with(gw.world) {
                val d = e[Mob].def
                if (d.lifeKind == LifeKind.WILDLIFE && (d.wildRole == WildRole.PREDATOR || d.wildRole == WildRole.APEX)) {
                    val et = e[Transform]; et.x = 20000f; et.y = 20000f; et.prevX = et.x; et.prevY = et.y
                }
            }
        }
        fun placeAdjacent() = with(gw.world) {
            val pt = predator!![Transform]; val qt = prey!![Transform]
            qt.x = pt.x + 18f; qt.y = pt.y; qt.prevX = qt.x; qt.prevY = qt.y
        }
        with(gw.world) { predator!![Mob].feedCd = 0f }
        placeAdjacent(); gw.world.update(1f / 60f)
        val afterFirst = with(gw.world) { prey!![Health].hp }
        assertTrue(with(gw.world) { predator!![Mob].feedCd } > 0f, "a bite should start the feed cooldown")
        placeAdjacent(); gw.world.update(1f / 60f) // immediately again — still cooling down
        val afterSecond = with(gw.world) { prey!![Health].hp }
        assertEquals(afterFirst, afterSecond, 1e-3f, "no second bite until the cooldown elapses")
    }
}
