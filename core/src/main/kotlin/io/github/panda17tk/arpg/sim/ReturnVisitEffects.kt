package io.github.panda17tk.arpg.sim

/**
 * How a remembered planet greets the next landing in its LAYOUT (LP v2.27): extra watch-guards by
 * the pad on a hostile world, unafraid herds + richer spoils on a grateful one, a thinned, hungrier
 * food web on a disrupted one. Spawn placement and starting minds only — no mid-fight intervention.
 */
data class SpawnTweaks(
    val extraGuardsAtPad: Int = 0,       // watch-guards placed beside the landing pad
    val herbivoreMul: Float = 1f,        // grazer/herd headcount multiplier (floored at 1 per group)
    val fleeSuppressed: Boolean = false, // herds/hatchlings no longer flee the player
    val bonusMaterialChance: Float = 0f, // extra chance of a blocks drop on kills
    val predatorStartHunger: Float = 0f, // predators/apexes start the visit this hungry
) {
    companion object { val NEUTRAL = SpawnTweaks() }
}

/**
 * The single entry point (FR-7.5) mapping a planet's remembered pressure to spawn tweaks.
 * All thresholds sit at the SocietyTuning register (0.5 / 0.6); a first visit (blank pressure)
 * is exactly NEUTRAL so no fresh-planet test can drift. Pure.
 */
object ReturnVisitEffects {
    const val HOSTILE_GATE = 0.6f     // hostility at/above this posts watch-guards at the pad
    const val MERCY_GATE = 0.5f       // mercy at/above this calms the wild + earns richer spoils
    const val DISRUPT_GATE = 0.5f     // disruption at/above this thins and starves the food web
    const val HERBIVORE_CUT = 0.6f    // grazer headcount multiplier on a disrupted world
    const val BONUS_MATERIAL = 0.35f  // extra blocks-drop chance on a grateful world
    const val PREDATOR_HUNGER = 0.5f  // starting hunger on a disrupted world (right at the hunt threshold)

    /** Whether the wild's herds/hatchlings stand their ground near the player (grateful worlds). */
    fun fleeSuppressed(p: SocietyPressure): Boolean = p.mercy >= MERCY_GATE

    /** v2.30 (10c): the halo tint a remembered planet wears in space — 0 neutral / 1 hostile / 2 grateful. */
    fun memoryTone(s: PlanetSocietyState): Int = when {
        s.hostility >= HOSTILE_GATE && s.hostility >= s.mercy -> 1
        s.mercy >= MERCY_GATE -> 2
        else -> 0
    }

    fun spawnTweaks(p: SocietyPressure): SpawnTweaks {
        val hostile = p.hostility >= HOSTILE_GATE
        val grateful = fleeSuppressed(p)
        val disrupted = p.ecologicalDisruption >= DISRUPT_GATE
        if (!hostile && !grateful && !disrupted) return SpawnTweaks.NEUTRAL
        return SpawnTweaks(
            extraGuardsAtPad = when {
                hostile && p.temperament == PlanetTemperament.VENGEFUL -> 2 // a vengeful world doubles the watch
                hostile -> 1
                else -> 0
            },
            herbivoreMul = if (disrupted) HERBIVORE_CUT else 1f,
            fleeSuppressed = grateful,
            bonusMaterialChance = if (grateful) BONUS_MATERIAL else 0f,
            predatorStartHunger = if (disrupted) PREDATOR_HUNGER else 0f,
        )
    }
}
