package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome

/**
 * Pure HUD text for the surface goal, now coloured by what the society remembers (LP v2.19). Severe ecology
 * events surface first (a slain child, a stalked nest), then the routine progression: defeat the masters,
 * claim the relic, return to the pad. One short line. The society defaults to "nothing happened yet", so the
 * plain objective shows until the ecosystem stirs.
 */
object SurfaceObjective {
    fun hudLine(
        biome: PlanetBiome,
        elitesAlive: Int,
        society: PlanetSocietyState = PlanetSocietyState(),
        context: PlanetContext = PlanetContext.NEUTRAL,
        remembered: Boolean = false,
    ): String {
        val name = biome.displayName
        return when {
            // 1. Child harm/kill — gravest; a children-sacred world words it as sacrilege.
            society.childKilled -> "$name：" + if (context.sacredThing == SacredThing.CHILDREN) "聖なる子が殺された　星は許さない" else "弱きものが失われた　部族は怒っている"
            society.childHarmed -> "$name：" + if (context.sacredThing == SacredThing.CHILDREN) "子らを傷つけた　星は怒っている" else "弱きものを傷つけた　守護者が奮い立つ"
            // 2. A remembered planet greets the returning player by reputation (shown briefly after landing).
            remembered && society.hostility >= 0.6f -> "$name：この星はあなたを敵として覚えている"
            remembered && society.mercy >= 0.5f -> "$name：この星はあなたへの借りを覚えている"
            // 3. A strike against what the planet holds sacred.
            context.sacredThing == SacredThing.APEX && society.apexKilled -> "$name：神獣は倒れた　星の均衡が崩れている"
            // 4. A kindness remembered.
            society.predatorKilledNearChild -> "$name：捕食者を退けた　森はあなたを見ている"
            // 5. Wild ecology events.
            society.hatchlingKilled || society.nestMotherKilled -> "$name：巣が荒らされた　野生がざわめく"
            society.apexKilled -> "$name：森の主を倒した　生態系が揺らいでいる"
            // 6. The elite objective.
            elitesAlive > 0 -> "$name：この星の主を倒せ（残り $elitesAlive）"
            // 7. Leader down, relic to claim.
            society.leaderDefeated && !society.relicClaimed -> "$name：主を倒した　素材を回収せよ"
            // 8. Back to the pad.
            else -> "${name}を制圧した　脱出パッドへ戻れ"
        }
    }
}
