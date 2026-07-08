package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.config.FamilyRole
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.config.WildRole
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.sim.PlanetContext
import io.github.panda17tk.arpg.sim.WorldMode
import io.github.panda17tk.arpg.sim.WorldState

/**
 * Turns ecology happenings into society memory (LP v2.19): 生態系は出来事を起こす。社会はそれを記憶する。
 * A wounded or slain child, a wild predator stalking the young, a predator driven off beside a child, and the
 * deaths of hatchlings / nest-guardians / the apex / the leader all land in [WorldState.society].
 *
 * Runs BEFORE [MobDamageSystem] so it sees a mob the tick it drops to 0 HP, just before it is reaped. Detection
 * is HP-delta + distance based — no exact damage-source tracking needed. Surface only.
 */
class EcologyEventSystem : IntervalSystem() {
    private val worldState: WorldState = world.inject()
    private val mobs by lazy { world.family { all(Mob, Transform, Health) } }
    private val prevHp = HashMap<Entity, Float>()

    override fun onTick() {
        if (worldState.mode != WorldMode.SURFACE) return
        val soc = worldState.society
        val ctx = worldState.context ?: PlanetContext.NEUTRAL // scales each deed by the planet's character
        // Pass 1: where are the society's children right now?
        val childX = ArrayList<Float>(); val childY = ArrayList<Float>()
        mobs.forEach { e ->
            with(world) { if (e[Mob].def.familyRole == FamilyRole.CHILD) { val t = e[Transform]; childX.add(t.x); childY.add(t.y) } }
        }
        fun nearAChild(x: Float, y: Float, r: Float): Boolean {
            val r2 = r * r
            for (i in childX.indices) { val dx = childX[i] - x; val dy = childY[i] - y; if (dx * dx + dy * dy < r2) return true }
            return false
        }
        // Pass 2: harm, death and threat events.
        mobs.forEach { e ->
            with(world) {
                val m = e[Mob]; val t = e[Transform]; val hp = e[Health].hp
                val prev = prevHp[e]
                val wild = m.def.lifeKind == LifeKind.WILDLIFE
                val wildHunter = wild && (m.def.wildRole == WildRole.PREDATOR || m.def.wildRole == WildRole.APEX)
                if (prev != null && prev > 0f && hp <= 0f) {
                    // died this tick (caught just before MobDamageSystem reaps it)
                    when {
                        m.def.familyRole == FamilyRole.CHILD && !m.fellByWild -> soc.onChildKilled(ctx) // v2.132: a wolf's kill is not the keeper's sin
                        m.def.familyRole == FamilyRole.KING -> soc.leaderDefeated = true
                    }
                    if (wild) when (m.def.wildRole) {
                        WildRole.APEX -> soc.onApexKilled(ctx)
                        WildRole.HATCHLING -> soc.onHatchlingKilled(ctx)
                        WildRole.NEST_GUARD -> soc.onNestMotherKilled(ctx)
                        else -> {}
                    }
                    if (wildHunter && nearAChild(t.x, t.y, REPEL_RANGE)) soc.onPredatorRepelledNearChild(ctx)
                } else if (prev != null && hp > 0f && hp < prev) {
                    if (m.def.familyRole == FamilyRole.CHILD) soc.onChildHarmed(ctx)
                }
                if (hp > 0f && wildHunter && nearAChild(t.x, t.y, THREAT_RANGE)) soc.onWildPredatorThreatenedChild()
                prevHp[e] = hp
            }
        }
    }

    companion object {
        private const val REPEL_RANGE = 100f  // a predator falling this close to a child = driven off beside the young
        private const val THREAT_RANGE = 120f // a live predator this close to a child threatens it
    }
}
