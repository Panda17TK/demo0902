package io.github.panda17tk.arpg.ecs.world

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import io.github.panda17tk.arpg.config.EnemyDef
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mob
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.components.Velocity

object MobFactory {
    /** Spawn one mob entity at (x,y). waveNum scales hp/speed (legacy makeMobFromKey). */
    fun spawn(world: World, def: EnemyDef, x: Float, y: Float, waveNum: Int = 1, hpScalePerWave: Float = 0f, speedScalePerWave: Float = 0f): Entity {
        val hs = 1f + (waveNum - 1) * hpScalePerWave
        val ss = 1f + (waveNum - 1) * speedScalePerWave
        val hp = def.hp * hs
        return world.entity {
            it += Transform(x = x, y = y, prevX = x, prevY = y)
            it += Body(def.w / 2f, def.h / 2f)
            it += Health(hp, hp)
            it += Velocity()
            it += Facing()
            it += Mob(
                kind = "${def.name}", def = def, speed = def.speed * ss,
                attackCd = FloatArray(def.attacks.size),
            )
        }
    }
}
