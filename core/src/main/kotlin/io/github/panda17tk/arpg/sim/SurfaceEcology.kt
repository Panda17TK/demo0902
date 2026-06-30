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

        // Wildlife layer: mute animals roaming the wider surface, scattered around the landing point (not the
        // camp), so the player crosses an ecosystem on the way to the society. Added last → the leader stays first.
        fun wild(key: String, count: Int, near: Float, far: Float) {
            repeat(count) {
                val a = rng.nextFloat() * TAU
                val d = near + (far - near) * rng.nextFloat()
                out.add(EcologyPlacement(key, clampX(spawnX + cos(a) * d), clampY(spawnY + sin(a) * d), passive = true))
            }
        }
        when (biome) {
            PlanetBiome.NATURE -> {
                wild("horn_deer", 4, WILD_NEAR, WILD_MID)       // a grazing herd
                wild("moss_hopper", 5, WILD_NEAR, WILD_FAR)     // skittish prey, scattered wide
                wild("fang_wolf", 2, WILD_MID, WILD_FAR)        // a wolf pack prowling the fringe
                wild("root_boar", 2, WILD_NEAR, WILD_MID)       // territorial tuskers
                wild("nest_mother", 1, WILD_MID, WILD_FAR)      // a nest guardian…
                wild("forest_hatchling", 3, WILD_MID, WILD_FAR) // …with her young nearby
                wild("forest_apex", 1, WILD_FAR, WILD_EDGE)     // the lone apex, far out
            }
            PlanetBiome.MAGMA -> wild("ember_moth", 4, WILD_NEAR, WILD_FAR)
            PlanetBiome.ICE -> wild("frost_hare", 5, WILD_NEAR, WILD_FAR)
            else -> {} // GAS / DEAD / LONELY: no wildlife yet (keeps GAS gravity-free + the asteroid sparse)
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
    private val WILD_NEAR = Tuning.TILE * 4f   // wildlife: nearest ring to the landing point
    private val WILD_MID = Tuning.TILE * 8f
    private val WILD_FAR = Tuning.TILE * 12f
    private val WILD_EDGE = Tuning.TILE * 16f  // the apex roams the far edge
}
