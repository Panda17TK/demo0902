package io.github.panda17tk.arpg.config

import io.github.panda17tk.arpg.planet.PlanetBiome
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameConfigWildlifeTest {
    private val enemies = GameConfig().enemies
    private val wildlife = enemies.values.filter { it.lifeKind == LifeKind.WILDLIFE }

    @Test fun `there is a wildlife roster`() {
        assertTrue(wildlife.size >= 6, "expected several wild species, got ${wildlife.size}")
    }

    @Test fun `wildlife never speak`() {
        for (w in wildlife) assertTrue(!w.canSpeak, "${w.name} should be mute")
    }

    @Test fun `every wild animal belongs to a biome — or swims the void`() {
        // v2.130 宙を泳ぐもの: space fish are the one sanctioned exception — biome-less, but
        // then they must ignore gravity (they swim the void, not fall through it).
        for (w in wildlife) {
            assertTrue(w.biome != null || w.gravityResponse == 0f, "${w.name} has no biome and still feels gravity")
        }
    }

    @Test fun `every wild animal has a food-web niche`() {
        for (w in wildlife) assertTrue(w.wildRole != WildRole.NONE, "${w.name} has no wild role")
    }

    @Test fun `the predator has attacks`() {
        val wolf = enemies.getValue("fang_wolf")
        assertTrue(wolf.wildRole == WildRole.PREDATOR && wolf.attacks.isNotEmpty(), "the wolf should hunt with attacks")
    }

    @Test fun `prey is fearful`() {
        val hopper = enemies.getValue("moss_hopper")
        assertTrue(hopper.wildRole == WildRole.PREY && hopper.fear >= 0.7f, "prey should be skittish")
    }

    @Test fun `nature carries the full food web`() {
        val roles = wildlife.filter { it.biome == PlanetBiome.NATURE }.map { it.wildRole }.toSet()
        for (r in listOf(WildRole.PREY, WildRole.HERD, WildRole.PREDATOR, WildRole.NEST_GUARD, WildRole.APEX)) {
            assertTrue(r in roles, "nature wildlife is missing $r")
        }
    }

    @Test fun `legacy enemies stay hostile and society creatures stay sapient`() {
        assertTrue(enemies.getValue("zombie").lifeKind == LifeKind.HOSTILE, "zombie should be a legacy hostile")
        assertTrue(enemies.getValue("beast_king").lifeKind == LifeKind.SAPIENT, "the beast king should be sapient")
    }
}
