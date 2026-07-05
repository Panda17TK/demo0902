package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.GameConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldBossTest {
    @Test fun `generic space roster has one boss and two midbosses`() {
        // Biome creatures (biome != null) live on their planet's surface and are gated out of space waves;
        // the generic space roster stays overlord (boss) + brute, warlock (midbosses). beast_king is now NATURE.
        val space = GameConfig().enemies.values.filter { it.biome == null }
        assertEquals(1, space.count { it.tier == "boss" }, "expected exactly one generic space boss (overlord)")
        // v2.41: artillery joins brute + warlock; v2.48: the auditor; v2.82: six more at the tail.
        assertEquals(10, space.count { it.tier == "midboss" }, "expected ten generic space midbosses (4 historic + 6 v2.82)")
    }

    @Test fun `the boss has the full attack kit`() {
        val overlord = GameConfig().enemies["overlord"]!!
        assertEquals("boss", overlord.tier)
        assertTrue(overlord.attacks.size >= 10, "overlord should have ~10 attacks")
        // Spot-check a few boss-only attack types are wired into the roster.
        val types = overlord.attacks.map { it.type }.toSet()
        assertTrue(types.containsAll(setOf("nova", "homing", "enrage", "summon")))
    }
}
