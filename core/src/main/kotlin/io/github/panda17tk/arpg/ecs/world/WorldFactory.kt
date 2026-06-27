package io.github.panda17tk.arpg.ecs.world

import com.github.quillraven.fleks.configureWorld
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.ecs.systems.MovementSystem
import io.github.panda17tk.arpg.ecs.systems.SnapshotSystem
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.sim.Tuning

object WorldFactory {
    fun create(input: InputState): GameWorld {
        val world = configureWorld {
            injectables { add(input) }
            systems {
                add(SnapshotSystem())
                add(MovementSystem())
            }
        }
        val player = world.entity {
            it += Transform(x = Tuning.VIEW_W / 2f, y = Tuning.VIEW_H / 2f)
            it += PlayerTag()
            it += Facing()
            it += Stamina()
        }
        return GameWorld(world, player)
    }
}
