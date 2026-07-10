package io.github.panda17tk.arpg.config

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.SurfaceEcology
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * v2.141 図鑑の救済: the field book (討伐図鑑) demands one personal kill of EVERY species —
 * so every species must actually be able to appear somewhere. A def added to the roster but
 * forgotten in the spawn lists would silently make the book un-completable forever.
 */
class SpawnCoverageTest {
    @Test fun `every fish the roster knows actually swims`() {
        val defs = GameConfig().enemies
        val strays = defs.filterValues { it.swims }.keys.filter { it !in SpaceFishRoster.ALL }
        assertTrue(strays.isEmpty(), "fish defs missing from SpaceFishRoster (they would never spawn): $strays")
        val ghosts = SpaceFishRoster.ALL.filter { it !in defs }
        assertTrue(ghosts.isEmpty(), "roster ids with no def: $ghosts")
    }

    @Test fun `every normal-tier species has a spawn path`() {
        val defs = GameConfig().enemies
        // the sky: every non-wildlife spaceborn surges/roams in by predicate; fish swim in via the roster
        val sky = defs.filterValues { it.biome == null && it.lifeKind != LifeKind.WILDLIFE }.keys + SpaceFishRoster.ALL
        // the surfaces: everything populate() can lay out, over every biome and a spread of seeds
        val surface = HashSet<String>()
        for (b in PlanetBiome.entries) for (seed in 1L..12L) {
            SurfaceEcology.populate(b, 1000f, 1000f, 3400f, 2200f, Rng(seed)).placements.forEach { surface += it.key }
        }
        // story seeds field their own casts (e.g. 沈黙の見守り via SILENT_MONASTERY) — cover each one
        for (b in PlanetBiome.entries) for (s in io.github.panda17tk.arpg.sim.PlanetStorySeed.entries) {
            val ctx = io.github.panda17tk.arpg.sim.PlanetContext(
                io.github.panda17tk.arpg.sim.PlanetTemperament.SILENT,
                io.github.panda17tk.arpg.sim.SacredThing.SILENCE, s,
            )
            SurfaceEcology.populate(b, 1000f, 1000f, 3400f, 2200f, Rng(1L), context = ctx).placements.forEach { surface += it.key }
        }
        // named specials with dedicated spawn code, and the vault/boss tiers spawned by their own systems
        val specials = setOf("rogue_drifter")
        val uncovered = defs.filterValues { it.tier == "normal" }.keys
            .filter { it !in sky && it !in surface && it !in specials }
        assertTrue(uncovered.isEmpty(), "species with no spawn path (the book could never close): $uncovered")
    }
}
