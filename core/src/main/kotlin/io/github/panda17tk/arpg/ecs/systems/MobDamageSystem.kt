package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.GameOver
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Mods
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.pathfinding.SpatialGrid

/** Rebuilds the mob spatial grid each tick and reaps dead mobs (kills++, lifesteal heal). */
class MobDamageSystem(private val grid: SpatialGrid<Entity>) :
    IteratingSystem(family { all(Mob, Transform, Health) }) {

    private val gameOver: GameOver = world.inject()
    private val players by lazy { world.family { all(PlayerTag, Health, Mods) } }

    override fun onTick() {
        grid.clear()
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val t = entity[Transform]
        if (entity[Health].hp <= 0f) {
            gameOver.kills++
            healOnKill()
            world -= entity
            return
        }
        grid.insert(entity, t.x, t.y)
    }

    /** Lifesteal: each kill heals the player by Mods.healOnKill, capped at hpMax (legacy). */
    private fun healOnKill() = with(world) {
        players.forEach { p ->
            val mods = p[Mods]
            if (mods.healOnKill > 0f) {
                val ph = p[Health]
                ph.hp = minOf(ph.hpMax, ph.hp + mods.healOnKill)
            }
        }
    }
}
