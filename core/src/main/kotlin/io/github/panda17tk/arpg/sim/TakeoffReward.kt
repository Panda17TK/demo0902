package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.planet.PlanetBiome

/** What the planet hands the departing player: blocks, an all-ammo top-up (fraction), and a small heal. */
data class RewardBundle(val blocks: Int = 0, val ammoPct: Float = 0f, val med: Float = 0f) {
    val isEmpty: Boolean get() = blocks == 0 && ammoPct == 0f && med == 0f
}

/**
 * The takeoff send-off (LP v2.29): a bundle computed from the visit's deeds and the star's feeling,
 * separate from the apex material boon (Consequence ×1.5) so the two never stack on one mechanism.
 * The one hard rule: a star whose child you killed gives you NOTHING — no matter what else you did.
 * Words live only here (§14.3). Pure.
 */
object TakeoffReward {
    const val LEADER_BLOCKS = 2     // the masters fell → the camp's stores open
    const val RELIC_AMMO_PCT = 0.10f // the relic hums → every ammo pool +10%
    const val MERCY_MED = 20f       // 民からの餞別: a little medicine…
    const val MERCY_BLOCKS = 1      // …and a spare block
    const val PROTECT_BLOCKS = 1    // children untouched on a world that watches them → gratitude

    const val TOAST_GIFT = "星からの餞別を受け取った"
    const val TOAST_NOTHING = "星は何も与えなかった"

    fun compute(s: PlanetSocietyState, biome: PlanetBiome, ctx: PlanetContext): RewardBundle {
        if (s.childKilled) return RewardBundle() // 罰則: the star gives nothing (FR-9.2)
        var blocks = 0
        var ammoPct = 0f
        var med = 0f
        if (s.leaderDefeated) blocks += LEADER_BLOCKS
        if (s.relicClaimed) ammoPct += RELIC_AMMO_PCT
        if (s.mercy >= ReturnVisitEffects.MERCY_GATE) { med += MERCY_MED; blocks += MERCY_BLOCKS }
        val watchesChildren = SurfaceGoalKind.PROTECT_CHILDREN in SurfaceGoals.forPlanet(biome, ctx)
        if (watchesChildren && !s.childHarmed) blocks += PROTECT_BLOCKS
        return RewardBundle(blocks, ammoPct, med)
    }

    /** The single toast line for the SPACE HUD after takeoff — null when there is nothing to say. */
    fun toastFor(bundle: RewardBundle, childKilled: Boolean): String? = when {
        childKilled -> TOAST_NOTHING
        bundle.isEmpty -> null
        else -> TOAST_GIFT
    }
}
