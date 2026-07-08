package io.github.panda17tk.arpg.sim

import io.github.panda17tk.arpg.config.EnemyDef
import io.github.panda17tk.arpg.config.FamilyRole
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.config.WildRole

/**
 * Pure rules for who eats whom in the wild food web. No libGDX/Fleks, so it is unit-testable;
 * [io.github.panda17tk.arpg.ecs.systems.WildPredationSystem] applies the bite, the hunger drop and the
 * cooldown. The watchword: 野生動物は配置物ではない。食べ、逃げ、守り、死ぬ。
 */
object Predation {
    /** Wild animals a hunter will eat (the player is never a predation target — only Threaten/Chase). */
    private val WILD_PREY = setOf(WildRole.PREY, WildRole.HERD, WildRole.HATCHLING, WildRole.SWARM, WildRole.SCHOOL)

    const val PREDATOR_BITE = 11f // a pack hunter's bite
    const val APEX_BITE = 22f     // an apex's bite

    /** Can [predator] (a wild hunter) actually prey on [prey] this encounter? */
    fun canPredate(predator: EnemyDef, prey: EnemyDef): Boolean {
        if (predator.lifeKind != LifeKind.WILDLIFE) return false
        if (prey === predator) return false
        val wildPrey = prey.lifeKind == LifeKind.WILDLIFE && prey.wildRole in WILD_PREY
        val sapientChild = prey.familyRole == FamilyRole.CHILD // a sapient society's young is fair game
        return when (predator.wildRole) {
            WildRole.PREDATOR -> wildPrey || sapientChild
            // An apex sits atop the chain: it also takes lesser predators.
            WildRole.APEX -> wildPrey || sapientChild || (prey.lifeKind == LifeKind.WILDLIFE && prey.wildRole == WildRole.PREDATOR)
            else -> false // scavengers (carrion — next round), nest-guards and grazers don't hunt
        }
    }

    /** Damage a single bite deals. */
    fun biteDamage(predator: EnemyDef): Float = when (predator.wildRole) {
        WildRole.APEX -> APEX_BITE
        WildRole.PREDATOR -> PREDATOR_BITE
        else -> 0f
    }

    /** How much hunger (0..1) a bite of [prey] sates for [predator]. */
    fun feedingGain(predator: EnemyDef, prey: EnemyDef): Float = when (predator.wildRole) {
        WildRole.APEX -> 0.4f
        WildRole.PREDATOR -> 0.3f
        else -> 0f
    }
}
