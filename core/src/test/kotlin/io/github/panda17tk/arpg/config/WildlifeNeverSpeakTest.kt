package io.github.panda17tk.arpg.config

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WildlifeNeverSpeakTest {
    private val wild = GameConfig().enemies.values.filter { it.lifeKind == LifeKind.WILDLIFE }

    @Test fun `no wildlife creature speaks`() {
        assertTrue(wild.isNotEmpty())
        assertTrue(wild.all { !it.canSpeak }, "wildlife must be mute (it builds an ecosystem, not a society)")
    }

    @Test fun `every wildlife creature lives on a biome`() {
        assertTrue(wild.all { it.biome != null }, "wildlife must belong to a planet biome")
    }
}
