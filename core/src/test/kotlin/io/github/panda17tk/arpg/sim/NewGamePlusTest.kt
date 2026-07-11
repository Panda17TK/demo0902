package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.world.GameWorld
import io.github.panda17tk.arpg.ecs.world.WorldFactory
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.160 周回の印II: clears deepen the next sky — capped, and never inside the walled worlds. */
class NewGamePlusTest {
    private val dt = 1f / 60f

    @Test fun `the mark is capped — a veteran account stays playable`() {
        assertEquals(0, NewGamePlus.depth(0))
        assertEquals(2, NewGamePlus.depth(1))
        assertEquals(6, NewGamePlus.depth(3))
        assertEquals(6, NewGamePlus.depth(99), "the ceiling holds")
        assertEquals(0, NewGamePlus.depth(-1), "corrupt prefs never soften the sky")
        assertEquals(0, NewGamePlus.levelBonus(0))
        assertEquals(2, NewGamePlus.levelBonus(2))
        assertEquals(3, NewGamePlus.levelBonus(99))
    }

    @Test fun `a marked account's sky starts deeper and its foes level up`() {
        val fresh = WorldFactory.create(InputState(), seed = 7L)
        val marked = WorldFactory.create(InputState(), seed = 7L, ngClears = 2)
        // the first wave's quota already reads the depth
        assertTrue(marked.waveState.toSpawn > fresh.waveState.toSpawn,
            "quota deepens (${marked.waveState.toSpawn} vs ${fresh.waveState.toSpawn})")
        // and every surge member spawns levelled. Surge spawns carry waveNum = wave + depth,
        // which separates them from the guards/drifters WorldFactory places at waveNum 1.
        fun surgeLevels(gw: GameWorld, surgeWaveNum: Int): List<Int> {
            repeat(180) { gw.world.update(dt) }
            val out = mutableListOf<Int>()
            with(gw.world) {
                gw.world.family { all(Mob) }.forEach { e ->
                    val m = e[Mob]
                    if (!m.drifter && m.def.tier == "normal" && m.def.biome == null &&
                        m.def.lifeKind != LifeKind.WILDLIFE && m.waveNum == surgeWaveNum
                    ) out += m.level
                }
            }
            return out
        }
        val f = surgeLevels(fresh, 1)
        val g = surgeLevels(marked, 1 + NewGamePlus.depth(2))
        assertTrue(f.isNotEmpty() && g.isNotEmpty(), "both surges vented (${f.size} vs ${g.size})")
        assertTrue(f.all { it == 1 }, "an unmarked sky spawns at level 1 ($f)")
        assertTrue(g.all { it == 3 }, "two clears add two levels ($g)")
    }

    @Test fun `zero clears is byte-identical to the shipped sky`() {
        val gw = WorldFactory.create(InputState(), seed = 5L)
        val c = GameConfig().waves
        assertEquals(minOf(c.maxQuota, c.baseQuota), gw.waveState.toSpawn)
        assertEquals(1, gw.waveState.num)
    }
}
