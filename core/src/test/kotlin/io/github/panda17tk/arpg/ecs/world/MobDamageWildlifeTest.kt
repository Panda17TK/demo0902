package io.github.panda17tk.arpg.ecs.world

import com.github.quillraven.fleks.Entity
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.WorldMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** "野生動物は配置物ではない。…死ぬ。" — but a wild death is ecology, not the player's score. */
class MobDamageWildlifeTest {
    private fun firstMob(gw: GameWorld, pred: (Mob) -> Boolean): Entity? {
        var found: Entity? = null
        gw.world.family { all(Mob, Health) }.forEach { e ->
            if (found == null && with(gw.world) { pred(e[Mob]) }) found = e
        }
        return found
    }

    @Test fun `a wild animal's death does not score a kill`() {
        val gw = WorldFactory.create(InputState(), seed = 3L, mode = WorldMode.SURFACE, biome = PlanetBiome.NATURE)
        val wild = firstMob(gw) { it.def.lifeKind == LifeKind.WILDLIFE }
        assertNotNull(wild, "nature should have wildlife")
        val before = gw.gameOver.kills
        with(gw.world) { wild!![Health].hp = -1f }
        gw.world.update(1f / 60f) // MobDamageSystem reaps it
        assertEquals(before, gw.gameOver.kills, "a wild animal's death must not tick the player's kills")
    }

    @Test fun `a normal enemy's death still scores a kill`() {
        val gw = WorldFactory.create(InputState(), seed = 1L) // SPACE — legacy hostile enemies
        val foe = firstMob(gw) { it.def.lifeKind != LifeKind.WILDLIFE }
        assertNotNull(foe, "the space stage should have enemies")
        val before = gw.gameOver.kills
        with(gw.world) { foe!![Health].hp = -1f }
        gw.world.update(1f / 60f)
        assertTrue(gw.gameOver.kills > before, "a normal enemy kill should count: ${gw.gameOver.kills} vs $before")
    }
}
