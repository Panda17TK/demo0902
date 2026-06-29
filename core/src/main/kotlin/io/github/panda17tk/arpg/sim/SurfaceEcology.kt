package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import kotlin.math.cos
import kotlin.math.sin

/** One inhabitant to place on a planet surface: its enemy key, world position, and whether it is a pacifist. */
data class EcologyPlacement(val key: String, val x: Float, val y: Float, val passive: Boolean = false)

/**
 * Pure "who lives here". Given a planet's biome and the player's landing point, lay out the planet's
 * inhabitants as a society: a tribe camp (king at the heart, guardians ringing it, young ranging out
 * front, children kept deepest), a scattered floating storm, a sparse haunted ruin, or a lone event
 * foe. Positions are clamped inside the arena; WorldFactory snaps each to a free floor tile when it spawns.
 */
object SurfaceEcology {
    fun populate(
        biome: PlanetBiome, spawnX: Float, spawnY: Float, worldW: Float, worldH: Float, rng: Rng,
    ): List<EcologyPlacement> {
        val out = ArrayList<EcologyPlacement>()
        // The settlement sits a short walk from the landing point, so the player walks into it.
        val campAng = rng.nextFloat() * TAU
        val campX = spawnX + cos(campAng) * CAMP_DIST
        val campY = spawnY + sin(campAng) * CAMP_DIST

        fun add(key: String, dist: Float, passive: Boolean = false) {
            val a = rng.nextFloat() * TAU
            val x = (campX + cos(a) * dist).coerceIn(MARGIN, worldW - MARGIN)
            val y = (campY + sin(a) * dist).coerceIn(MARGIN, worldH - MARGIN)
            out.add(EcologyPlacement(key, x, y, passive))
        }

        when (biome) {
            PlanetBiome.NATURE -> {
                add("beast_king", 0f)
                add("spore_shaman", INNER)
                repeat(2) { add("beast_whelp", INNER) }     // children kept at the heart
                repeat(2) { add("forest_guardian", GUARD) } // guardians ring the camp
                repeat(2) { add("young_beast", OUTER) }     // young range out front
            }
            PlanetBiome.MAGMA -> {
                add("volcano_king", 0f)
                repeat(2) { add("obsidian_guard", GUARD) }
                repeat(2) { add("lava_thrower", OUTER) }
                repeat(2) { add("ember_imp", OUTER) }
            }
            PlanetBiome.ICE -> {
                add("ice_queen", 0f)
                add("snow_stalker", GUARD)
                repeat(2) { add("ice_spearman", OUTER) }
                repeat(3) { add("frostling", GUARD) }
            }
            PlanetBiome.GAS -> {
                // No tight camp — a drifting storm spread across the cloud sea.
                add("storm_core", 0f)
                repeat(3) { add("storm_orb", OUTER + FAR * rng.nextFloat()) }
                repeat(2) { add("wind_jelly", FAR) }
                add("gravity_wraith", OUTER)
            }
            PlanetBiome.DEAD -> {
                // Sparse but tough: the oracle, a sentinel, and a couple of mindless husks.
                add("dead_oracle", 0f)
                add("ruin_guard", GUARD)
                repeat(2) { add("bone_drone", FAR) }
            }
            PlanetBiome.LONELY -> {
                // An event encounter: a single duel foe, sometimes a peaceful monk nearby.
                if (rng.nextFloat() < 0.5f) add("exiled_king", 0f) else add("lost_soldier", 0f)
                if (rng.nextFloat() < 0.5f) add("star_monk", OUTER, passive = true)
            }
        }
        return out
    }

    private const val TAU = 6.2831855f
    private val CAMP_DIST = Tuning.TILE * 7f // the settlement's heart, this far from the landing point
    private val INNER = Tuning.TILE * 1.4f   // children / shaman, tucked at the core
    private val GUARD = Tuning.TILE * 2.6f    // guardians ring the camp
    private val OUTER = Tuning.TILE * 3.8f    // young / ranged range out front
    private val FAR = Tuning.TILE * 5.2f      // scattered drifters
    private val MARGIN = Tuning.TILE * 2f     // keep clear of the arena's outer walls
}
