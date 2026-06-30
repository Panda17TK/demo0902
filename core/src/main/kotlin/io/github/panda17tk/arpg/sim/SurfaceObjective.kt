package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome

/**
 * Pure HUD text for the surface goal, now coloured by what the society remembers (LP v2.19). Severe ecology
 * events surface first (a slain child, a stalked nest), then the routine progression: defeat the masters,
 * claim the relic, return to the pad. One short line. The society defaults to "nothing happened yet", so the
 * plain objective shows until the ecosystem stirs.
 */
object SurfaceObjective {
    fun hudLine(biome: PlanetBiome, elitesAlive: Int, society: PlanetSocietyState = PlanetSocietyState()): String {
        val name = biome.displayName
        return when {
            society.childKilled -> "$name：弱きものが失われた　部族は怒っている"
            society.childHarmed -> "$name：弱きものを傷つけた　守護者が奮い立つ"
            society.predatorKilledNearChild -> "$name：捕食者を退けた　森はあなたを見ている"
            society.hatchlingKilled -> "$name：巣が荒らされた　野生がざわめく"
            society.apexKilled -> "$name：森の主を倒した　生態系が揺らいでいる"
            elitesAlive > 0 -> "$name：この星の主を倒せ（残り $elitesAlive）"
            society.leaderDefeated && !society.relicClaimed -> "$name：主を倒した　素材を回収せよ"
            else -> "${name}を制圧した　脱出パッドへ戻れ"
        }
    }
}
