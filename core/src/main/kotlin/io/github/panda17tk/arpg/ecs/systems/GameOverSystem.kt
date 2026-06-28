package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.GameOver
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.PlayerTag

/** Flags the run as over once the player's HP is depleted (consumed by GameScreen's overlay). */
class GameOverSystem : IteratingSystem(family { all(PlayerTag, Health) }) {
    private val gameOver: GameOver = world.inject()

    override fun onTickEntity(entity: Entity) {
        if (entity[Health].hp <= 0f) gameOver.isOver = true
    }
}
