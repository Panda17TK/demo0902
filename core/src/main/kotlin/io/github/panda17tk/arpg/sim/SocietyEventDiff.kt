package io.github.panda17tk.arpg.sim

/**
 * Read-only change detection for the surface event feed (LP v2.24). The existing emitters
 * (EcologyEventSystem / PickupSystem) mutate [PlanetSocietyState] directly — and some calls repeat
 * every tick — so the feed fires on **state edges** instead: booleans on their rising edge only,
 * gauges only when they cross a threshold (a later climb past it stays silent). Deltas come out in
 * severity order (SocietyDelta's declaration order). Pure.
 */
object SocietyEventDiff {
    const val HOSTILITY_CROSS = 0.6f // hostility crossing this → 「この星はあなたを敵と見なした」
    const val MERCY_CROSS = 0.5f     // mercy crossing this → 「この星はあなたに借りを感じている」

    fun diff(before: PlanetSocietyState, after: PlanetSocietyState, ctx: PlanetContext): List<PlanetEvent> {
        val out = ArrayList<PlanetEvent>(2)
        fun add(d: SocietyDelta) = out.add(PlanetEventLines.line(d, ctx))
        val childKilledNow = !before.childKilled && after.childKilled
        if (childKilledNow) add(SocietyDelta.CHILD_KILLED)
        if (!before.apexKilled && after.apexKilled) add(SocietyDelta.APEX_KILLED)
        if (!before.nestMotherKilled && after.nestMotherKilled) add(SocietyDelta.NEST_MOTHER_KILLED)
        if (!before.hatchlingKilled && after.hatchlingKilled) add(SocietyDelta.HATCHLING_KILLED)
        // A killed child also sets childHarmed — the kill line already covers it, so don't double-report.
        if (!before.childHarmed && after.childHarmed && !childKilledNow) add(SocietyDelta.CHILD_HARMED)
        if (!before.predatorKilledNearChild && after.predatorKilledNearChild) add(SocietyDelta.PREDATOR_REPELLED)
        if (!before.relicClaimed && after.relicClaimed) add(SocietyDelta.RELIC_CLAIMED)
        if (!before.leaderDefeated && after.leaderDefeated) add(SocietyDelta.LEADER_DEFEATED)
        if (before.hostility < HOSTILITY_CROSS && after.hostility >= HOSTILITY_CROSS) add(SocietyDelta.HOSTILITY_CROSSED)
        if (before.mercy < MERCY_CROSS && after.mercy >= MERCY_CROSS) add(SocietyDelta.MERCY_CROSSED)
        return out
    }
}
