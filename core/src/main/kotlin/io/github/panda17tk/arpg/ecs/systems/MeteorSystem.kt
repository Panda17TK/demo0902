package io.github.panda17tk.arpg.ecs.systems

import com.badlogic.gdx.graphics.Color
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.Fx
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Meteor
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.world.Pickups
import io.github.panda17tk.arpg.sim.WaveEvents
import kotlin.math.hypot

/**
 * v2.87 流星群 — each rock falls for [Meteor.fall] telegraphed seconds (the renderer draws the
 * growing shadow), then lands: whoever stands inside the ring is hurt, the rock sheds a little
 * dust, and the impact borrows the staged-death show. Damage numbers stay honest via Fx pops.
 */
class MeteorSystem : IteratingSystem(family { all(Meteor, Transform) }) {
    private val fx: Fx = world.inject()
    private val players by lazy { world.family { all(PlayerTag, Transform, Health) } }
    private val mobs by lazy { world.family { all(Mob, Transform, Health) } }

    override fun onTickEntity(entity: Entity) {
        val m = entity[Meteor]
        m.fall -= deltaTime
        if (m.fall > 0f) return
        val t = entity[Transform]
        val r = WaveEvents.METEOR_RADIUS

        players.forEach { p ->
            val pt = p[Transform]
            if (hypot(pt.x - t.x, pt.y - t.y) < r) {
                val h = p[Health]
                if (h.iTime <= 0f) { h.hp -= WaveEvents.METEOR_DMG; h.iTime = 0.6f }
            }
        }
        mobs.forEach { e ->
            val mt = e[Transform]
            if (hypot(mt.x - t.x, mt.y - t.y) < r) e[Health].hp -= WaveEvents.METEOR_MOB_DMG
        }
        Pickups.spawn(world, "dust", WaveEvents.METEOR_DUST, t.x, t.y)
        fx.spawnDeath(t.x, t.y, ROCK, big = false)
        fx.spawnSparks(t.x, t.y, 10, EMBER)
        fx.addShake(0.12f, 5f)
        world -= entity
    }

    companion object {
        private val ROCK = Color.valueOf("b09a7a")
        private val EMBER = Color.valueOf("ffb060")
    }
}
