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

    private fun wolfOf(gw: GameWorld) = find(gw) { it.def.lifeKind == LifeKind.WILDLIFE && it.def.wildRole == WildRole.PREDATOR }
    private fun apexOf(gw: GameWorld) = find(gw) { it.def.lifeKind == LifeKind.WILDLIFE && it.def.wildRole == WildRole.APEX }
    private fun preyOf(gw: GameWorld) = find(gw) { it.def.lifeKind == LifeKind.WILDLIFE && it.def.wildRole == WildRole.PREY }

    @Test fun `a hungry hunter bites adjacent prey and feeds`() {
        val gw = natureWorld()
        val wolf = wolfOf(gw); val prey = preyOf(gw)
        assertNotNull(wolf); assertNotNull(prey)
        val hpBefore = with(gw.world) {
            val wt = wolf!![Transform]; val wm = wolf[Mob]
            wm.feedCd = 0f; wm.hunger = 1f
            val qt = prey!![Transform]
            qt.x = wt.x + 18f; qt.y = wt.y; qt.prevX = qt.x; qt.prevY = qt.y
            prey[Health].hp
        }
        gw.world.update(1f / 60f)
        with(gw.world) {
            assertTrue(prey!![Health].hp < hpBefore, "a hungry wolf should bite: ${prey[Health].hp} vs $hpBefore")
            assertTrue(wolf!![Mob].hunger < 0.95f, "feeding should reduce hunger, was ${wolf[Mob].hunger}")
        }
    }

    @Test fun `a fed predator ignores prey that merely wandered past`() {
        val gw = natureWorld()
        val wolf = wolfOf(gw); val prey = preyOf(gw)
        assertNotNull(wolf); assertNotNull(prey)
        val hpBefore = with(gw.world) {
            val wt = wolf!![Transform]; val wm = wolf[Mob]
            wm.feedCd = 0f; wm.hunger = 0f // sated
            val qt = prey!![Transform]
            qt.x = wt.x + 18f; qt.y = wt.y; qt.prevX = qt.x; qt.prevY = qt.y
            prey[Health].hp
        }
        gw.world.update(1f / 60f)
        with(gw.world) { assertEquals(hpBefore, prey!![Health].hp, 1e-3f, "a fed wolf should not bite") }
    }

    @Test fun `an apex bites a lesser predator regardless of appetite`() {
        val gw = natureWorld()
        val apex = apexOf(gw); val wolf = wolfOf(gw)
        assertNotNull(apex); assertNotNull(wolf)
        val hpBefore = with(gw.world) {
            val at = apex!![Transform]; apex[Mob].feedCd = 0f; apex[Mob].hunger = 0f // appetite irrelevant for an apex
            val wt = wolf!![Transform]
            wt.x = at.x + 16f; wt.y = at.y; wt.prevX = wt.x; wt.prevY = wt.y
            wolf[Health].hp
        }
        gw.world.update(1f / 60f)
        with(gw.world) { assertTrue(wolf!![Health].hp < hpBefore, "an apex should bite a wolf: ${wolf[Health].hp} vs $hpBefore") }
    }

    @Test fun `the feed cooldown stops every-frame chewing`() {
        val gw = natureWorld()
        val wolf = wolfOf(gw); val prey = preyOf(gw)
        assertNotNull(wolf); assertNotNull(prey)
        // shove every OTHER wild hunter far away so only our wolf can reach the prey
        gw.world.family { all(Mob, Transform) }.forEach { e ->
            if (e != wolf && e != prey) with(gw.world) {
                val d = e[Mob].def
                if (d.lifeKind == LifeKind.WILDLIFE && (d.wildRole == WildRole.PREDATOR || d.wildRole == WildRole.APEX)) {
                    val et = e[Transform]; et.x = 20000f; et.y = 20000f; et.prevX = et.x; et.prevY = et.y
                }
            }
        }
        fun arm() = with(gw.world) {
            val wm = wolf!![Mob]; wm.feedCd = 0f; wm.hunger = 1f
            val wt = wolf[Transform]; val qt = prey!![Transform]
            qt.x = wt.x + 18f; qt.y = wt.y; qt.prevX = qt.x; qt.prevY = qt.y
        }
        arm(); gw.world.update(1f / 60f)
        val afterFirst = with(gw.world) { prey!![Health].hp }
        assertTrue(with(gw.world) { wolf!![Mob].feedCd } > 0f, "a bite should start the feed cooldown")
        // re-arm position + hunger but NOT feedCd → the cooldown must still block a second bite
        with(gw.world) {
            wolf!![Mob].hunger = 1f
            val wt = wolf[Transform]; val qt = prey!![Transform]
            qt.x = wt.x + 18f; qt.y = wt.y; qt.prevX = qt.x; qt.prevY = qt.y
        }
        gw.world.update(1f / 60f)
        with(gw.world) { assertEquals(afterFirst, prey!![Health].hp, 1e-3f, "no second bite until the cooldown elapses") }
    }
}
