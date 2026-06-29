package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.pathfinding.FlowField
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.floor

/**
 * Rebuilds the flow-field toward the player on a configurable interval so mobs path
 * to the moving player. Uses IteratingSystem over the player family (safe for Fleks 2.8).
 */
class FlowRebuildSystem : IteratingSystem(family { all(PlayerTag, Transform) }) {
    private val map: TileMap = world.inject()
    private val flow: FlowField = world.inject()
    private val config: GameConfig = world.inject()

    private var timer = 0f

    override fun onTick() {
        timer -= deltaTime
        if (timer > 0f) return
        timer = config.ai.flowRebuildInterval
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        val t = entity[Transform]
        // Cap BFS radius so pathfinding cost stays O(MAX_DIST²) even on the huge 32× maps.
        flow.rebuild(map, floor(t.x / Tuning.TILE).toInt(), floor(t.y / Tuning.TILE).toInt(), MAX_DIST)
    }

    companion object {
        const val MAX_DIST = 70
    }
}
