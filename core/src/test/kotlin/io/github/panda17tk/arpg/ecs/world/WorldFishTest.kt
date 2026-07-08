package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.WorldMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.130 宙を泳ぐもの: fish-like wildlife in SPACE — schooling, harmless to the waves. */
class WorldFishTest {
    private val fishIds = listOf("star_sardine", "void_koi", "lantern_angler")

    private fun wildCount(gw: GameWorld): Int = with(gw.world) {
        var n = 0
        gw.world.family { all(Mob) }.forEach { if (it[Mob].def.lifeKind == LifeKind.WILDLIFE) n++ }
        n
    }

    @Test fun `some space skies host fish, deterministically per seed`() {
        var hosted = 0
        for (seed in 1L..20L) {
            val a = WorldFactory.create(InputState(), seed = seed)
            val b = WorldFactory.create(InputState(), seed = seed)
            assertEquals(wildCount(a), wildCount(b), "seed $seed schools the same sky twice")
            if (wildCount(a) > 0) hosted++
        }
        assertTrue(hosted in 1..19, "fish visit some skies but not all (got $hosted/20)")
    }

    @Test fun `the fish are true void wildlife, and surfaces never host them`() {
        val defs = GameConfig().enemies
        for (id in fishIds) {
            val d = defs.getValue(id)
            assertEquals(LifeKind.WILDLIFE, d.lifeKind, "$id is wildlife")
            assertEquals(null, d.biome, "$id lives in space")
            assertEquals(0f, d.gravityResponse, "$id swims past the planets' pull")
        }
        val surf = WorldFactory.create(InputState(), seed = 2L, mode = WorldMode.SURFACE, biome = PlanetBiome.NATURE)
        with(surf.world) {
            surf.world.family { all(Mob) }.forEach { e ->
                assertTrue(e[Mob].def.id !in fishIds, "no space fish on a surface (found ${e[Mob].def.id})")
            }
        }
    }

    @Test fun `fish never enter the hostile spawn pools`() {
        // mirrors the DesyncSurge / drifter / guard pool predicates — lifeKind keeps wildlife out
        val pool = GameConfig().enemies
            .filterValues { it.tier == "normal" && it.biome == null && it.lifeKind == LifeKind.HOSTILE }.keys
        assertTrue(fishIds.none { it in pool }, "wildlife must not surge")
        assertTrue(pool.isNotEmpty(), "the hostile pool still stands")
    }

    @Test fun `the player's wild kill enters the book — a predator's hunt does not`() {
        val gw = WorldFactory.create(InputState(), seed = 4L)
        val def = GameConfig().enemies.getValue("star_sardine")
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        val mine = MobFactory.spawn(gw.world, def, px + 400f, py)
        val hunted = MobFactory.spawn(gw.world, def, px + 460f, py)
        val killsBefore = gw.gameOver.kills
        with(gw.world) {
            mine[Health].hp = 0f                                  // the keeper's own kill
            hunted[Mob].fellByWild = true; hunted[Health].hp = 0f // a predator's hunt
        }
        gw.world.update(1f / 60f)
        assertEquals(1, gw.gameOver.killsByKind["star_sardine"], "the field book takes the keeper's kill only")
        assertEquals(killsBefore, gw.gameOver.kills, "wild deaths still never tick the score")
    }
}
