package io.github.panda17tk.arpg.sim

/**
 * Pure consequence-of-deeds rewards. The clearest one: felling a planet's apex unbalances its food web
 * (the disruption is recorded by [PlanetSocietyState.onApexKilled]) but yields richer remains — so the
 * material boon claimed afterwards is stronger. No libGDX/Fleks → unit-testable.
 */
object Consequence {
    /** Multiplier applied to a claimed material's boon: a slain apex makes the spoils richer. */
    fun materialMultiplier(s: PlanetSocietyState): Float = if (s.apexKilled) APEX_BONUS else 1f

    const val APEX_BONUS = 1.5f // a slain apex → ~50% stronger material (paid for in ecological disruption)
}
