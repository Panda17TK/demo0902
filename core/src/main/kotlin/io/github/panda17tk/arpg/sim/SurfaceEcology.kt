package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.planet.PlanetBiome
import kotlin.math.cos
import kotlin.math.sin

/** One inhabitant to place on a planet surface: its enemy key, world position, and whether it is a pacifist. */
data class EcologyPlacement(val key: String, val x: Float, val y: Float, val passive: Boolean = false)

/** A built landmark on the surface — the leader's seat or scattered ruins — drawn as a real ground object. */
enum class FacilityKind { CAMP, CRATER, DAIS, EYE, SHRINE, RUIN }

data class Facility(val kind: FacilityKind, val x: Float, val y: Float, val radius: Float)

/** A planet's inhabitants plus the structures they live among. */
data class Society(val placements: List<EcologyPlacement>, val facilities: List<Facility>)

/**
 * Pure "who lives here, and where". Given a planet's biome and the player's landing point, lay out the
 * planet's inhabitants as a society — a tribe camp (king at the heart, guardians ringing it, young out
 * front, children deepest), a scattered floating storm, a sparse ruin, or a lone event foe — and the
 * structures they sit among (a camp, a volcanic crater throne, an icy dais, a storm eye, a shrine, ruins).
 * Positions are clamped inside the arena; WorldFactory snaps creatures to a free floor tile when it spawns.
 */
object SurfaceEcology {
    fun populate(
        biome: PlanetBiome, spawnX: Float, spawnY: Float, worldW: Float, worldH: Float, rng: Rng,
    ): Society {
        val out = ArrayList<EcologyPlacement>()
        val fac = ArrayList<Facility>()
        // The settlement sits a short walk from the landing point, so the player walks into it.
        val campAng = rng.nextFloat() * TAU
        val campX = spawnX + cos(campAng) * CAMP_DIST
        val campY = spawnY + sin(campAng) * CAMP_DIST
        fun clampX(x: Float) = x.coerceIn(MARGIN, worldW - MARGIN)
        fun clampY(y: Float) = y.coerceIn(MARGIN, worldH - MARGIN)

        fun add(key: String, dist: Float, passive: Boolean = false) {
            val a = rng.nextFloat() * TAU
            out.add(EcologyPlacement(key, clampX(campX + cos(a) * dist), clampY(campY + sin(a) * dist), passive))
        }
        val cx = clampX(campX); val cy = clampY(campY) // the leader's seat — the central facility sits here

        when (biome) {
            PlanetBiome.NATURE -> {
                add("beast_king", 0f)
                add("spore_shaman", INNER)
                repeat(2) { add("beast_whelp", INNER) }     // children kept at the heart
                repeat(2) { add("forest_guardian", GUARD) } // guardians ring the camp
                repeat(2) { add("young_beast", OUTER) }     // young range out front
                fac.add(Facility(FacilityKind.CAMP, cx, cy, CAMP_R))
            }
            PlanetBiome.MAGMA -> {
                add("volcano_king", 0f)
                repeat(2) { add("obsidian_guard", GUARD) }
                repeat(2) { add("lava_thrower", OUTER) }
                repeat(2) { add("ember_imp", OUTER) }
                fac.add(Facility(FacilityKind.CRATER, cx, cy, CAMP_R)) // the king reigns from the crater
            }
            PlanetBiome.ICE -> {
                add("ice_queen", 0f)
                add("snow_stalker", GUARD)
                repeat(2) { add("ice_spearman", OUTER) }
                repeat(3) { add("frostling", GUARD) }
                fac.add(Facility(FacilityKind.DAIS, cx, cy, CAMP_R)) // the queen's icy dais
            }
            PlanetBiome.GAS -> {
                add("storm_core", 0f)
                repeat(3) { add("storm_orb", OUTER + FAR * rng.nextFloat()) }
                repeat(2) { add("wind_jelly", FAR) }
                add("gravity_wraith", OUTER)
                fac.add(Facility(FacilityKind.EYE, cx, cy, CAMP_R * 1.2f)) // the eye of the storm
            }
            PlanetBiome.DEAD -> {
                add("dead_oracle", 0f)
                add("ruin_guard", GUARD)
                repeat(2) { add("bone_drone", FAR) }
                fac.add(Facility(FacilityKind.SHRINE, cx, cy, CAMP_R * 0.7f)) // the oracle's shrine
                repeat(3) {
                    val a = rng.nextFloat() * TAU; val d = GUARD + FAR * rng.nextFloat()
                    fac.add(Facility(FacilityKind.RUIN, clampX(campX + cos(a) * d), clampY(campY + sin(a) * d), Tuning.TILE * 0.9f))
                }
            }
            PlanetBiome.LONELY -> {
                if (rng.nextFloat() < 0.5f) add("exiled_king", 0f) else add("lost_soldier", 0f)
                if (rng.nextFloat() < 0.5f) add("star_monk", OUTER, passive = true)
                fac.add(Facility(FacilityKind.SHRINE, cx, cy, CAMP_R * 0.6f)) // a lonely waymark
            }
        }
        return Society(out, fac)
    }

    private const val TAU = 6.2831855f
    private val CAMP_DIST = Tuning.TILE * 7f // the settlement's heart, this far from the landing point
    private val INNER = Tuning.TILE * 1.4f   // children / shaman, tucked at the core
    private val GUARD = Tuning.TILE * 2.6f    // guardians ring the camp
    private val OUTER = Tuning.TILE * 3.8f    // young / ranged range out front
    private val FAR = Tuning.TILE * 5.2f      // scattered drifters
    private val MARGIN = Tuning.TILE * 2f     // keep clear of the arena's outer walls
    private val CAMP_R = Tuning.TILE * 2.2f   // central facility radius
}
