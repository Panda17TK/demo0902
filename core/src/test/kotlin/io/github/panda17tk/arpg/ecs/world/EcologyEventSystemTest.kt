package io.github.panda17tk.arpg.ecs.world

import com.github.quillraven.fleks.Entity
import io.github.panda17tk.arpg.config.FamilyRole
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.WorldMode
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The ecosystem makes events; the society remembers them — EcologyEventSystem → WorldState.society. */
class EcologyEventSystemTest {
    private val dt = 1f / 60f
    private fun world() = WorldFactory.create(InputState(), seed = 3L, mode = WorldMode.SURFACE, biome = PlanetBiome.NATURE)
    private fun find(gw: GameWorld, pred: (Mob) -> Boolean): Entity? {
        var found: Entity? = null
        gw.world.family { all(Mob, Transform, Health) }.forEach { e -> if (found == null && with(gw.world) { pred(e[Mob]) }) found = e }
        return found
    }

    @Test fun `a child losing HP marks childHarmed`() {
        val gw = world()
        val child = find(gw) { it.def.familyRole == FamilyRole.CHILD }; assertNotNull(child)
        gw.world.update(dt) // populate prev-HP
        with(gw.world) { child!![Health].hp -= 8f } // wound it, still alive
        gw.world.update(dt)
        assertTrue(gw.worldState.society.childHarmed, "childHarmed should be set")
    }

    @Test fun `a wild predator beside a child marks wildPredatorThreatenedChild`() {
        val gw = world()
        val child = find(gw) { it.def.familyRole == FamilyRole.CHILD }
        val wolf = find(gw) { it.def.lifeKind == LifeKind.WILDLIFE && it.def.wildRole == WildRole.PREDATOR }
        assertNotNull(child); assertNotNull(wolf)
        with(gw.world) { val ct = child!![Transform]; val wt = wolf!![Transform]; wt.x = ct.x + 60f; wt.y = ct.y; wt.prevX = wt.x; wt.prevY = wt.y }
        gw.world.update(dt)
        assertTrue(gw.worldState.society.wildPredatorThreatenedChild)
    }

    @Test fun `a predator dying beside a child marks predatorKilledNearChild`() {
        val gw = world()
        val child = find(gw) { it.def.familyRole == FamilyRole.CHILD }
        val wolf = find(gw) { it.def.lifeKind == LifeKind.WILDLIFE && it.def.wildRole == WildRole.PREDATOR }
        assertNotNull(child); assertNotNull(wolf)
        fun place() = with(gw.world) { val ct = child!![Transform]; val wt = wolf!![Transform]; wt.x = ct.x + 50f; wt.y = ct.y; wt.prevX = wt.x; wt.prevY = wt.y }
        place(); gw.world.update(dt) // prev-HP populated, wolf beside the child
        with(gw.world) { wolf!![Health].hp = -1f }; place()
        gw.world.update(dt)
        assertTrue(gw.worldState.society.predatorKilledNearChild)
    }

    @Test fun `a hatchling dying marks hatchlingKilled`() {
        val gw = world()
        val h = find(gw) { it.def.lifeKind == LifeKind.WILDLIFE && it.def.wildRole == WildRole.HATCHLING }; assertNotNull(h)
        gw.world.update(dt)
        with(gw.world) { h!![Health].hp = -1f }
        gw.world.update(dt)
        assertTrue(gw.worldState.society.hatchlingKilled)
    }

    @Test fun `the apex dying marks apexKilled`() {
        val gw = world()
        val a = find(gw) { it.def.lifeKind == LifeKind.WILDLIFE && it.def.wildRole == WildRole.APEX }; assertNotNull(a)
        gw.world.update(dt)
        with(gw.world) { a!![Health].hp = -1f }
        gw.world.update(dt)
        assertTrue(gw.worldState.society.apexKilled)
    }
}
