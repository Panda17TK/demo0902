package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.EBullet
import io.github.panda17tk.arpg.ecs.components.Smoke
import io.github.panda17tk.arpg.ecs.components.Transform

/** Smoke clouds tick down and despawn; while alive they erase enemy bullets that enter them. */
class SmokeSystem : IntervalSystem() {
    private val smokes by lazy { world.family { all(Smoke, Transform) } }
    private val ebullets by lazy { world.family { all(EBullet, Transform) } }

    override fun onTick() {
        val dt = deltaTime
        with(world) {
            smokes.forEach { s ->
                val sm = s[Smoke]; sm.t += dt
                if (sm.t >= sm.life) world -= s
            }
            ebullets.forEach { e ->
                val et = e[Transform]
                smokes.forEach { s ->
                    val sm = s[Smoke]
                    if (sm.t >= sm.life) return@forEach
                    val st = s[Transform]
                    val dx = et.x - st.x; val dy = et.y - st.y
                    if (dx * dx + dy * dy <= sm.radius * sm.radius) { world -= e; return@forEach }
                }
            }
        }
    }
}
