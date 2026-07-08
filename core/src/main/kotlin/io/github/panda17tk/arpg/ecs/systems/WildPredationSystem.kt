package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.config.WildState
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.pathfinding.SpatialGrid
import io.github.panda17tk.arpg.sim.Predation
import io.github.panda17tk.arpg.sim.WildAI
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.hypot

/**
 * Wild predators biting their prey — the 食べ ("eat") of 食べ、逃げ、守り、死ぬ. Kept separate from
 * [WildlifeSystem] (which only senses + moves) so neither system bloats. Only WILDLIFE predators/apexes
 * act; prey they kill is reaped by [MobDamageSystem] next tick. The player is never bitten here — toward
 * the player a predator only Threatens/Chases (decided in WildAI). Pure rules live in [Predation].
 */
class WildPredationSystem(private val mobGrid: SpatialGrid<Entity>) :
    IteratingSystem(family { all(Mob, Transform, Health) }) {

    override fun onTickEntity(entity: Entity) {
        val pred = entity[Mob]
        if (pred.def.lifeKind != LifeKind.WILDLIFE) return
        if (pred.def.wildRole != WildRole.PREDATOR && pred.def.wildRole != WildRole.APEX) return
        // Only an actively hunting animal bites — a fed wolf doesn't snap at prey that merely wandered past.
        // An apex rules its turf, so it bites whenever it's closing in; a lesser predator must also be hungry.
        val hunting = pred.def.wildRole == WildRole.APEX ||
            pred.wildState == WildState.Chase || pred.wildState == WildState.Stalk || pred.wildState == WildState.Hunt
        if (!hunting) return
        if (pred.def.wildRole == WildRole.PREDATOR && pred.hunger < WildAI.HUNGRY) return
        if (pred.feedCd > 0f) { pred.feedCd -= deltaTime; return } // one bite per cooldown — skip the scan while chewing

        val t = entity[Transform]
        var prey: Entity? = null
        var preyD = Float.MAX_VALUE
        mobGrid.forNearby(t.x, t.y, BITE_RANGE) { other ->
            if (other == entity) return@forNearby
            with(world) {
                if (!Predation.canPredate(pred.def, other[Mob].def)) return@forNearby
                val ot = other[Transform]
                val d = hypot(ot.x - t.x, ot.y - t.y)
                if (d < preyD) { preyD = d; prey = other }
            }
        }
        val target = prey ?: return
        if (preyD > BITE_RANGE) return // a grid bucket can reach a touch past the radius — confirm the real distance
        with(world) {
            target[Health].hp -= Predation.biteDamage(pred.def)
            if (target[Health].hp <= 0f) target[Mob].fellByWild = true // v2.130 図鑑: the wild's own kill, not the player's
            pred.hunger = (pred.hunger - Predation.feedingGain(pred.def, target[Mob].def)).coerceAtLeast(0f)
        }
        pred.feedCd = FEED_CD
    }

    companion object {
        private val BITE_RANGE = Tuning.TILE * 1.25f // close enough to land a bite (~40px)
        private const val FEED_CD = 0.8f             // seconds between bites on the same meal
    }
}
