package io.github.panda17tk.arpg.sim

/**
 * What a planet's society remembers about this surface visit. 生態系は出来事を起こす。社会はそれを記憶する。
 * (The ecosystem makes events; the society remembers them.) [io.github.panda17tk.arpg.ecs.systems.EcologyEventSystem]
 * detects the events and calls the apply-methods here; the HUD ([SurfaceObjective]) and guardians read the result.
 *
 * Persisted per planet across landings via [PlanetMemoryBook]: a landing seeds the surface society from the
 * planet's remembered state ([copyState]) and a takeoff folds the visit back in ([mergeFrom]).
 * The three float gauges accumulate (clamped 0..1): how angry the tribe is, how much it owes the player, and how
 * shaken the food web is.
 */
class PlanetSocietyState(
    var childHarmed: Boolean = false,
    var childKilled: Boolean = false,
    var wildPredatorThreatenedChild: Boolean = false,
    var predatorKilledNearChild: Boolean = false,
    var hatchlingKilled: Boolean = false,
    var nestMotherKilled: Boolean = false,
    var apexKilled: Boolean = false,
    var surrenderKilled: Int = 0,
    var surrenderedSpared: Int = 0,
    var leaderDefeated: Boolean = false,
    var relicClaimed: Boolean = false,
    var hostility: Float = 0f,            // how angry the tribe is at the player
    var mercy: Float = 0f,                // how much goodwill the player has earned
    var ecologicalDisruption: Float = 0f, // how shaken the food web is
) {
    // Gauge deltas now run through SocietyImpact, scaled by the planet's [ctx] (temperament + sacred). A NEUTRAL
    // context (the default) leaves the base deltas unchanged, so no-arg callers behave exactly as before.

    /** The player wounded one of the tribe's young — guardians stir (harder on a vengeful / child-sacred world). */
    fun onChildHarmed(ctx: PlanetContext = PlanetContext.NEUTRAL) {
        childHarmed = true; hostility = bump(hostility, SocietyImpact.childHarmed(CHILD_HARM, ctx))
    }

    /** The player killed one of the tribe's young — the gravest offence. */
    fun onChildKilled(ctx: PlanetContext = PlanetContext.NEUTRAL) {
        childKilled = true; childHarmed = true; hostility = bump(hostility, SocietyImpact.childKilled(CHILD_KILL, ctx))
    }

    /** A wild predator is stalking a child — recorded so guardians can be roused. */
    fun onWildPredatorThreatenedChild() { wildPredatorThreatenedChild = true }

    /** A predator fell near a child (often the player driving it off) — the tribe takes note kindly. */
    fun onPredatorRepelledNearChild(ctx: PlanetContext = PlanetContext.NEUTRAL) {
        predatorKilledNearChild = true; mercy = bump(mercy, SocietyImpact.predatorRepelled(PRED_REPEL, ctx))
    }

    /** Defenceless young of the wild were killed — the wilderness stirs. */
    fun onHatchlingKilled(ctx: PlanetContext = PlanetContext.NEUTRAL) {
        hatchlingKilled = true; ecologicalDisruption = bump(ecologicalDisruption, SocietyImpact.nestHarmed(NEST_HARM, ctx))
    }

    /** A nest-guardian fell — its young are exposed, the wild grows restless. */
    fun onNestMotherKilled(ctx: PlanetContext = PlanetContext.NEUTRAL) {
        nestMotherKilled = true
        ecologicalDisruption = bump(ecologicalDisruption, SocietyImpact.nestHarmed(NEST_HARM, ctx))
        hostility = bump(hostility, SocietyImpact.nestHarmed(NEST_MOTHER_HOST, ctx))
    }

    /** The apex was slain — the food web is thrown badly out of balance (worse where the apex is sacred). */
    fun onApexKilled(ctx: PlanetContext = PlanetContext.NEUTRAL) {
        apexKilled = true; ecologicalDisruption = bump(ecologicalDisruption, SocietyImpact.apexKilled(APEX_DISRUPT, ctx))
    }

    /** The planet's relic was carried off — a slight affront, sharper where the relic is held sacred. */
    fun onRelicClaimed(ctx: PlanetContext = PlanetContext.NEUTRAL) {
        relicClaimed = true; hostility = bump(hostility, SocietyImpact.relicClaimed(RELIC_CLAIM, ctx))
    }

    private fun bump(v: Float, by: Float) = (v + by).coerceIn(0f, 1f)

    companion object {
        private const val CHILD_HARM = 0.3f       // base hostility from wounding a child
        private const val CHILD_KILL = 0.8f       // base hostility from killing a child
        private const val PRED_REPEL = 0.4f       // base mercy from driving a predator off a child
        private const val NEST_HARM = 0.3f        // base disruption from a slain hatchling / nest mother
        private const val NEST_MOTHER_HOST = 0.2f // base hostility from a slain nest mother
        private const val APEX_DISRUPT = 0.5f     // base disruption from felling the apex
        private const val RELIC_CLAIM = 0.15f     // base hostility from carrying off the relic
    }

    /** A deep copy — used to seed a fresh surface visit from a planet's remembered state without aliasing it. */
    fun copyState(): PlanetSocietyState = PlanetSocietyState(
        childHarmed, childKilled, wildPredatorThreatenedChild, predatorKilledNearChild,
        hatchlingKilled, nestMotherKilled, apexKilled, surrenderKilled, surrenderedSpared,
        leaderDefeated, relicClaimed, hostility, mercy, ecologicalDisruption,
    )

    /**
     * Fold a finished surface visit's [surface] society back into this persistent record.
     * Booleans OR (a deed once done is never forgotten); counters and the 0..1 gauges keep the MAX.
     * Because a visit is seeded from this record via [copyState], the visit's values already include the past,
     * so MAX persists the latest without double-counting and keeps repeated merges idempotent.
     */
    fun mergeFrom(surface: PlanetSocietyState) {
        childHarmed = childHarmed || surface.childHarmed
        childKilled = childKilled || surface.childKilled
        wildPredatorThreatenedChild = wildPredatorThreatenedChild || surface.wildPredatorThreatenedChild
        predatorKilledNearChild = predatorKilledNearChild || surface.predatorKilledNearChild
        hatchlingKilled = hatchlingKilled || surface.hatchlingKilled
        nestMotherKilled = nestMotherKilled || surface.nestMotherKilled
        apexKilled = apexKilled || surface.apexKilled
        leaderDefeated = leaderDefeated || surface.leaderDefeated
        relicClaimed = relicClaimed || surface.relicClaimed
        surrenderKilled = maxOf(surrenderKilled, surface.surrenderKilled)
        surrenderedSpared = maxOf(surrenderedSpared, surface.surrenderedSpared)
        hostility = maxOf(hostility, surface.hostility)
        mercy = maxOf(mercy, surface.mercy)
        ecologicalDisruption = maxOf(ecologicalDisruption, surface.ecologicalDisruption)
    }
}
