package io.github.panda17tk.arpg.ecs.systems

import com.badlogic.gdx.graphics.Color
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.Fx
import io.github.panda17tk.arpg.ecs.components.GameOver
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Mods
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.world.Pickups
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.pathfinding.SpatialGrid

/** Rebuilds the mob spatial grid each tick and reaps dead mobs (kills++, lifesteal heal). */
class MobDamageSystem(private val grid: SpatialGrid<Entity>) :
    IteratingSystem(family { all(Mob, Transform, Health) }) {

    private val gameOver: GameOver = world.inject()
    private val fx: Fx = world.inject()
    private val rng: Rng = world.inject()
    private val players by lazy { world.family { all(PlayerTag, Health, Mods) } }

    override fun onTick() {
        grid.clear()
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val t = entity[Transform]
        if (entity[Health].hp <= 0f) {
            val mob = entity[Mob]
            // A wild animal's death is part of the ecosystem, not the player's score — no kill count,
            // no lifesteal, no loot farm (a wolf eating a deer must not tick the player's tally).
            val wild = mob.def.lifeKind == LifeKind.WILDLIFE
            if (!wild) {
                gameOver.kills++
                healOnKill()
            }
            val big = mob.tier != "normal"
            fx.spawnDeath(t.x, t.y, Color.valueOf(mob.def.color.removePrefix("#")), big)
            fx.addShake(if (big) 0.25f else 0.08f, if (big) 9f else 3.5f)
            if (!wild) Pickups.dropOnKill(world, rng, t.x, t.y, big)
            // A planet's king/elite drops a biome material (a core/relic) that grants the player a small boon.
            val biome = mob.def.biome
            if (biome != null && big) Pickups.spawn(world, "mat_" + biome.name.lowercase(), 1, t.x, t.y)
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
