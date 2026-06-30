package io.github.panda17tk.arpg.planet

import kotlinx.serialization.Serializable

/**
 * The ecological **type** of a Living Planet — what kind of world it is. Distinct from
 * [io.github.panda17tk.arpg.map.Biome], which is a per-block terrain *material* (rock/grass/snow/magma)
 * driving block colour and touch effects. A planet's biome shapes its surface arena (see SurfaceStages),
 * its landing HUD label ([displayName]), its surface-wide physics (e.g. ICE slips, MAGMA burns) and
 * which creatures inhabit it (see GameConfig enemy rosters + SurfaceEcology).
 */
@Serializable
enum class PlanetBiome(val displayName: String) {
    NATURE("自然惑星"),
    MAGMA("火山惑星"),
    ICE("氷惑星"),
    GAS("ガス惑星"),
    DEAD("死の惑星"),
    LONELY("孤独な小惑星"),
}
