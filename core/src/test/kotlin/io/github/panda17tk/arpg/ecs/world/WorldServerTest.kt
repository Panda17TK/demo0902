package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.MemoryCoreLog
import io.github.panda17tk.arpg.sim.WaveEvents
import io.github.panda17tk.arpg.sim.WorldMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.48 惑星サーバー: the custodial machinery, the purge roster, and every surface's memory core. */
class WorldServerTest {
    @Test fun `the preservation machinery joined the space roster`() {
        val enemies = GameConfig().enemies
        for (key in listOf("custodian", "indexer", "quarantine")) {
            val def = enemies[key]
            assertNotNull(def, "$key must exist")
            assertEquals("normal", def!!.tier)
            assertNull(def.biome, "$key patrols space")
        }
        val auditor = enemies["auditor"]!!
        assertEquals("midboss", auditor.tier)
        assertTrue(auditor.intelligence >= 0.85f, "the audit process has read every fight")
        // Its summon references a real roster key (the custodian drone).
        val minion = auditor.attacks.firstOrNull { it.type == "summon" }?.minion
        assertNotNull(enemies[minion], "auditor summons a real unit, got $minion")
    }

    @Test fun `every purge key resolves to a space normal`() {
        val enemies = GameConfig().enemies
        for (key in WaveEvents.PURGE_KEYS) {
            val def = enemies[key]
            assertTrue(def != null && def.tier == "normal" && def.biome == null, "$key must be a space normal")
        }
    }

    @Test fun `every surface places its memory core, deterministically`() {
        val a = WorldFactory.create(InputState(), seed = 7L, mode = WorldMode.SURFACE, biome = PlanetBiome.ICE)
        val b = WorldFactory.create(InputState(), seed = 7L, mode = WorldMode.SURFACE, biome = PlanetBiome.ICE)
        assertNotNull(a.worldState.memoryCore)
        assertEquals(a.worldState.memoryCore, b.worldState.memoryCore)
        assertTrue(!a.worldState.coreLogShown, "the core has not spoken yet")
    }

    @Test fun `space has no memory core`() {
        val gw = WorldFactory.create(InputState(), seed = 7L)
        assertNull(gw.worldState.memoryCore)
    }

    @Test fun `the core speaks deterministically, in its own register`() {
        val one = MemoryCoreLog.lineFor(42L, PlanetBiome.DEAD)
        assertEquals(one, MemoryCoreLog.lineFor(42L, PlanetBiome.DEAD))
        assertTrue(one.startsWith("記憶核: "))
        // Across many planets and biomes, the pool actually varies.
        val lines = (0L..40L).flatMap { id -> PlanetBiome.entries.map { MemoryCoreLog.lineFor(id, it) } }.toSet()
        assertTrue(lines.size > 5, "expected variety, got ${lines.size}")
    }

    @Test fun `the revelation deepens with the star systems (v2_49)`() {
        // System 1: routine logs. Systems 2-3: the network starts matching. 4+: it says it plainly.
        val early = (0L..30L).map { MemoryCoreLog.lineFor(it, PlanetBiome.ICE, system = 1) }
        val aware = (0L..30L).map { MemoryCoreLog.lineFor(it, PlanetBiome.ICE, system = 2) }
        val reveal = (0L..30L).map { MemoryCoreLog.lineFor(it, PlanetBiome.ICE, system = 5) }
        assertTrue(early.none { it.contains("最終保守員") }, "system 1 must not spoil the reveal")
        assertTrue(aware.any { it.contains("照合") }, "systems 2-3 hint at the signature match")
        assertTrue(reveal.any { it.contains("最終保守員") || it.contains("出力") }, "system 4+ states the truth")
        // Deterministic at every stage.
        assertEquals(
            MemoryCoreLog.lineFor(7L, PlanetBiome.GAS, system = 5),
            MemoryCoreLog.lineFor(7L, PlanetBiome.GAS, system = 5),
        )
    }
}
