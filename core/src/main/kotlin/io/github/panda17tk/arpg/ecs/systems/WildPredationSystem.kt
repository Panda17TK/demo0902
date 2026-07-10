package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.config.WildState
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.Gear
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mods
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Velocity
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
 * act; prey they kill is reaped by [MobDamageSystem] next tick. v2.132: a BRAVE hunter on the hunt
 * snaps at the KEEPER too (same bite, same cooldown); the timid still only threaten. Rules: [Predation].
 */
class WildPredationSystem(private val mobGrid: SpatialGrid<Entity>) :
    IteratingSystem(family { all(Mob, Transform, Health) }) {
    private val config: GameConfig = world.inject()
    private val fx: io.github.panda17tk.arpg.ecs.components.Fx = world.inject()
    private val difficulty: io.github.panda17tk.arpg.sim.Difficulty = world.inject()
    private val players by lazy { world.family { all(PlayerTag, Transform, Health, Velocity) } }

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
        // v2.132 近接 / v2.138 公正な野生: a BRAVE hunter mid-hunt lunges at the keeper — but the
        // lunge is TELEGRAPHED: a warn ring and a short windup before the teeth close, the same
        // fairness a hostile's charge windup gives. Step out of reach and the lunge whiffs.
        if (pred.def.bravery >= Predation.BRAVE) {
            var engaged = false
            players.forEach { e ->
                if (engaged) return@forEach
                val pt = e[Transform]; val ph = e[Health]
                val d = hypot(pt.x - t.x, pt.y - t.y)
                if (d < BITE_RANGE * ENGAGE_MUL) {
                    engaged = true
                    if (pred.biteWindup <= 0f) {
                        pred.biteWindup = BITE_WINDUP
                        fx.spawnWarnRing(t.x, t.y)
                    } else {
                        pred.biteWindup -= deltaTime
                        if (pred.biteWindup <= 0f) {
                            pred.biteWindup = 0f
                            if (d < BITE_RANGE && ph.iTime <= 0f) {
                                ph.hp -= Predation.biteDamage(pred.def) *
                                    (e.getOrNull(Gear)?.loadout?.damageTakenMul ?: 1f) * difficulty.dmgTakenMul *
                                    (e.getOrNull(Mods)?.armorMul ?: 1f)
                                ph.iTime = config.ai.iFrameContact
                                val dd = d.coerceAtLeast(0.0001f)
                                e[Velocity].vx += (pt.x - t.x) / dd * pred.def.contactKB
                                e[Velocity].vy += (pt.y - t.y) / dd * pred.def.contactKB
                                pred.feedCd = FEED_CD
                            }
                        }
                    }
                }
            }
            if (engaged) return // committed to the keeper — no prey scan this tick
            pred.biteWindup = 0f // the keeper slipped away; the lunge resets
        }
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
        private const val BITE_WINDUP = 0.45f        // v2.138: the lunge telegraphs this long before the teeth close
        private const val ENGAGE_MUL = 1.6f          // ...and commits when the keeper is within range × this
    }
}
