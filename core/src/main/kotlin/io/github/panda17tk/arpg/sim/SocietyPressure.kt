package io.github.panda17tk.arpg.sim

/**
 * A flattened, read-only snapshot of how a planet's society currently feels, combining its remembered
 * [PlanetSocietyState] with its [PlanetContext] (temperament + sacred). Systems read this to nudge — never
 * dominate — behaviour: 社会はそれを記憶する。そして次の客を、その記憶で迎える。
 */
data class SocietyPressure(
    val hostility: Float = 0f,
    val mercy: Float = 0f,
    val ecologicalDisruption: Float = 0f,
    val childHarmed: Boolean = false,
    val childKilled: Boolean = false,
    val predatorKilledNearChild: Boolean = false,
    val apexKilled: Boolean = false,
    val temperament: PlanetTemperament = PlanetTemperament.SILENT,
    val sacredThing: SacredThing = SacredThing.SILENCE,
)

/** Project the per-visit memory + the planet's character into a single pressure snapshot. */
fun PlanetSocietyState.toPressure(ctx: PlanetContext): SocietyPressure = SocietyPressure(
    hostility = hostility,
    mercy = mercy,
    ecologicalDisruption = ecologicalDisruption,
    childHarmed = childHarmed,
    childKilled = childKilled,
    predatorKilledNearChild = predatorKilledNearChild,
    apexKilled = apexKilled,
    temperament = ctx.temperament,
    sacredThing = ctx.sacredThing,
)

/** Small AI nudges derived from society pressure (sapient creatures). All inert under a blank/neutral pressure. */
data class AiPressure(
    val skipWarn: Boolean,   // very angry world → fewer warnings, straight to aggression
    val rallyEager: Boolean, // a child was killed (or harmed on a vengeful world) → guardians rally readily
    val mercyBoost: Float,   // a forgiving/indebted world → creatures beg/yield a touch sooner
    val gratitude: Boolean,  // the player drove a predator off a child → not treated as an immediate threat
)

/** Small wildlife nudges derived from society pressure (the wild layer). Inert under a blank/neutral pressure. */
data class WildPressure(
    val fearMul: Float,   // a shaken food web → prey/herds spook sooner
    val hungerMul: Float, // a slain apex / a hungry world → predators hunt sooner
)

/**
 * Pure mapping from [SocietyPressure] to the small per-system nudges. Kept gentle on purpose — society memory
 * colours behaviour, it does not instantly dominate it. No libGDX/Fleks → unit-testable.
 */
object SocietyTuning {
    fun ai(p: SocietyPressure): AiPressure = AiPressure(
        skipWarn = p.hostility >= HOSTILE_SKIP && p.temperament != PlanetTemperament.PROUD, // proud worlds always warn first
        rallyEager = p.childKilled || (p.temperament == PlanetTemperament.VENGEFUL && p.childHarmed),
        mercyBoost = if (p.mercy >= MERCY_HIGH) MERCY_BOOST * (if (p.temperament == PlanetTemperament.GENTLE) 1.5f else 1f) else 0f,
        gratitude = p.predatorKilledNearChild,
    )

    fun wild(p: SocietyPressure): WildPressure = WildPressure(
        fearMul = 1f + FEAR_PER_DISRUPT * p.ecologicalDisruption,
        hungerMul = (if (p.apexKilled) APEX_HUNGER else 1f) * (if (p.temperament == PlanetTemperament.HUNGRY) HUNGRY_MUL else 1f),
    )

    private const val HOSTILE_SKIP = 0.6f    // hostility at/above this skips the warning
    private const val MERCY_HIGH = 0.5f      // mercy at/above this eases creatures toward begging
    private const val MERCY_BOOST = 0.12f    // how much the mercy threshold rises (×1.5 on gentle worlds)
    private const val FEAR_PER_DISRUPT = 0.5f // up to +50% fear at full ecological disruption
    private const val APEX_HUNGER = 1.3f     // a slain apex leaves predators ~30% hungrier
    private const val HUNGRY_MUL = 1.4f      // a HUNGRY world's predators grow hungry ~40% faster
}
