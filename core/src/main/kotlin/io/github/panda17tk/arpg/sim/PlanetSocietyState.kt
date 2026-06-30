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
    /** The player wounded one of the tribe's young — guardians stir. */
    fun onChildHarmed() { childHarmed = true; hostility = bump(hostility, 0.3f) }

    /** The player killed one of the tribe's young — the gravest offence. */
    fun onChildKilled() { childKilled = true; childHarmed = true; hostility = bump(hostility, 0.8f) }

    /** A wild predator is stalking a child — recorded so guardians can be roused. */
    fun onWildPredatorThreatenedChild() { wildPredatorThreatenedChild = true }

    /** A predator fell near a child (often the player driving it off) — the tribe takes note kindly. */
    fun onPredatorRepelledNearChild() { predatorKilledNearChild = true; mercy = bump(mercy, 0.4f) }

    /** Defenceless young of the wild were killed — the wilderness stirs. */
    fun onHatchlingKilled() { hatchlingKilled = true; ecologicalDisruption = bump(ecologicalDisruption, 0.3f) }

    /** A nest-guardian fell — its young are exposed, the wild grows restless. */
    fun onNestMotherKilled() { nestMotherKilled = true; ecologicalDisruption = bump(ecologicalDisruption, 0.3f); hostility = bump(hostility, 0.2f) }

    /** The apex was slain — the food web is thrown badly out of balance. */
    fun onApexKilled() { apexKilled = true; ecologicalDisruption = bump(ecologicalDisruption, 0.5f) }

    private fun bump(v: Float, by: Float) = (v + by).coerceIn(0f, 1f)

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
