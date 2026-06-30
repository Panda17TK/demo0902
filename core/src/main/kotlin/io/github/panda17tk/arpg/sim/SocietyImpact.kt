package io.github.panda17tk.arpg.sim

/**
 * Scales a base society-impact magnitude by a planet's [PlanetContext]: a VENGEFUL world takes harm harder, a
 * GENTLE one forgives and rewards mercy more, and striking at whatever the planet holds sacred cuts deepest.
 * Pure math (callers decide which gauge moves and the sign) → unit-testable.
 */
object SocietyImpact {
    fun childHarmed(base: Float, ctx: PlanetContext): Float = anger(base, ctx, SacredThing.CHILDREN)
    fun childKilled(base: Float, ctx: PlanetContext): Float = anger(base, ctx, SacredThing.CHILDREN)
    fun apexKilled(base: Float, ctx: PlanetContext): Float = anger(base, ctx, SacredThing.APEX)
    fun nestHarmed(base: Float, ctx: PlanetContext): Float = anger(base, ctx, SacredThing.NEST)
    fun relicClaimed(base: Float, ctx: PlanetContext): Float = anger(base, ctx, SacredThing.RELIC)

    /** Mercy (e.g. driving a predator off a child) lands harder on gentle/ancient worlds, less on vengeful ones. */
    fun predatorRepelled(base: Float, ctx: PlanetContext): Float =
        base * mercyMul(ctx.temperament) * sacredMul(ctx.sacredThing, SacredThing.CHILDREN)

    /** An "anger" deed: scaled by how easily the temperament takes offence, then by whether it struck the sacred. */
    private fun anger(base: Float, ctx: PlanetContext, struck: SacredThing): Float =
        base * angerMul(ctx.temperament) * sacredMul(ctx.sacredThing, struck)

    private fun angerMul(t: PlanetTemperament): Float = when (t) {
        PlanetTemperament.VENGEFUL -> 1.6f
        PlanetTemperament.PROUD -> 1.2f
        PlanetTemperament.FEARFUL -> 1.1f
        PlanetTemperament.GENTLE -> 0.7f
        else -> 1f
    }

    private fun mercyMul(t: PlanetTemperament): Float = when (t) {
        PlanetTemperament.GENTLE -> 1.6f
        PlanetTemperament.ANCIENT -> 1.2f
        PlanetTemperament.VENGEFUL -> 0.7f
        else -> 1f
    }

    private fun sacredMul(actual: SacredThing, struck: SacredThing): Float = if (actual == struck) 1.8f else 1f
}
