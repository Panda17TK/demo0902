package io.github.panda17tk.arpg.config

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameConfigExpandedWildlifeTest {
    private val enemies = GameConfig().enemies
    private val newKeys = listOf(
        "ash_lizard", "basalt_ram", "lava_serpent", "crater_hatchling",
        "ice_muskox", "white_stalker", "frost_worm", "sleeping_calf",
        "cloud_plankton", "storm_ray", "thunder_eel", "gravity_whelp",
        "bone_rat", "ash_crow", "grave_mimic", "ruin_parasite",
        "star_moth", "old_hound", "last_beast", "silent_watcher",
    )

    @Test fun `every new wildlife exists and is WILDLIFE with a biome`() {
        for (k in newKeys) {
            val d = enemies[k]
            assertNotNull(d, "missing wildlife def: $k")
            assertTrue(d!!.lifeKind == LifeKind.WILDLIFE, "$k is not WILDLIFE")
            assertNotNull(d.biome, "$k has no biome")
        }
    }

    @Test fun `predators and apex have attacks`() {
        for (k in newKeys) {
            val d = enemies[k]!!
            if (d.wildRole == WildRole.PREDATOR || d.wildRole == WildRole.APEX) {
                assertTrue(d.attacks.isNotEmpty(), "$k (a hunter) needs attacks")
            }
        }
    }

    @Test fun `prey and herd are fearful`() {
        for (k in newKeys) {
            val d = enemies[k]!!
            if (d.wildRole == WildRole.PREY || d.wildRole == WildRole.HERD) {
                assertTrue(d.fear > 0f, "$k (prey/herd) needs fear")
            }
        }
    }

    @Test fun `hatchlings are fragile`() {
        val hatch = newKeys.map { enemies[it]!! }.filter { it.wildRole == WildRole.HATCHLING }
        assertTrue(hatch.isNotEmpty())
        assertTrue(hatch.all { it.hp <= 30f }, "hatchlings should be frail")
    }

    @Test fun `apex creatures are rare and territorial`() {
        val apex = newKeys.map { enemies[it]!! }.filter { it.wildRole == WildRole.APEX }
        assertTrue(apex.isNotEmpty())
        assertTrue(apex.all { it.territoryRadius > 0f }, "an apex holds territory")
    }
}
