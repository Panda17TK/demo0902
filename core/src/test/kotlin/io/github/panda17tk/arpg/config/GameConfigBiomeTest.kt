package io.github.panda17tk.arpg.config

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameConfigBiomeTest {
    private val enemies = GameConfig().enemies

    @Test fun `every planet biome has its own inhabitants`() {
        for (b in PlanetBiome.values()) {
            assertTrue(enemies.values.any { it.biome == b }, "no creatures for $b")
        }
    }

    @Test fun `generic space enemies are not tagged to a planet biome`() {
        for (key in listOf("zombie", "spitter", "stalker", "brute", "warlock", "overlord")) {
            assertNull(enemies[key]?.biome, "$key should stay a generic space enemy")
        }
    }

    @Test fun `the nature planet has a child a guardian and a king`() {
        val nature = enemies.values.filter { it.biome == PlanetBiome.NATURE }
        assertTrue(nature.any { it.familyRole == FamilyRole.CHILD }, "nature needs a child")
        assertTrue(nature.any { it.familyRole == FamilyRole.GUARDIAN }, "nature needs a guardian")
        assertTrue(nature.any { it.familyRole == FamilyRole.KING }, "nature needs a king")
    }

    @Test fun `every planet biome fields an elite that drops a relic`() {
        // Elites = midboss/boss tiers; those are what MobDamageSystem rewards with a planet material.
        for (b in PlanetBiome.values()) {
            assertTrue(
                enemies.values.any { it.biome == b && it.tier != "normal" },
                "no elite (midboss/boss) for $b",
            )
        }
    }

    @Test fun `summoned minions resolve to real enemy keys`() {
        for (def in enemies.values) {
            for (atk in def.attacks) {
                if (atk.type == "summon" && atk.minion.isNotEmpty()) {
                    assertNotNull(enemies[atk.minion], "${def.name} summons missing '${atk.minion}'")
                }
            }
        }
    }
}
