package io.github.panda17tk.arpg.combat

/**
 * v2.80 近接コンボ — swing again on the beat and the blade grows: wider, harder, brighter.
 * The beat itself tightens as the combo climbs (the window to chain shrinks), but each step
 * also shortens the cooldown and raises the damage, so a kept rhythm is strictly more DPS.
 * Pure math: the system feeds presses in, multipliers come out.
 */
object MeleeCombo {
    const val MAX_STEP = 5

    /** The cooldown after landing [step] — the beat quickens as the combo climbs. */
    fun cooldown(step: Int, baseCd: Float): Float = baseCd * (1f - 0.08f * (clamp(step) - 1))

    /** After the cooldown ends, how long the NEXT press still chains — tightening per step. */
    fun chainWindow(step: Int): Float = 0.55f - 0.075f * (clamp(step) - 1)

    fun dmgMul(step: Int): Float = 1f + 0.18f * (clamp(step) - 1)   // ×1.00 → ×1.72
    fun reachMul(step: Int): Float = 1f + 0.11f * (clamp(step) - 1) // ×1.00 → ×1.44
    fun arcMul(step: Int): Float = 1f + 0.08f * (clamp(step) - 1)   // the swing opens up

    /** FX budget per step — the show grows with the rhythm. */
    fun sparks(step: Int): Int = 4 + clamp(step) * 5

    /** The step the next swing lands at, given the current [step] and whether it chained. */
    fun nextStep(step: Int, chained: Boolean): Int =
        if (chained) (step + 1).coerceAtMost(MAX_STEP) else 1

    private fun clamp(step: Int) = step.coerceIn(1, MAX_STEP)
}
