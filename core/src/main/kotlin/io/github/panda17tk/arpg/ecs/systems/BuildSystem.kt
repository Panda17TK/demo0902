package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.Body
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Materials
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.map.Tiles
import io.github.panda17tk.arpg.pathfinding.FlowField
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.floor

/** On the placeWall input edge: place a destructible wall on the tile in front of the player. */
class BuildSystem : IteratingSystem(family { all(PlayerTag, Transform, Facing, Body, Materials) }) {
    private val input: InputState = world.inject()
    private val map: TileMap = world.inject()
    private val flow: FlowField = world.inject()

    override fun onTickEntity(entity: Entity) {
        if (!input.placeWall) return
        input.placeWall = false // v2.153: the buffered tap is consumed by this attempt
        val t = entity[Transform]
        val f = entity[Facing]
        val mats = entity[Materials]
        val b = entity[Body]
        if (mats.blocks <= 0) return

        // Place on the next tile in the facing/aim direction so it never overlaps the player (no stuck).
        val frontX = t.x + f.x * (Tuning.TILE * 0.9f)
        val frontY = t.y + f.y * (Tuning.TILE * 0.9f)
        val tx = floor(frontX / Tuning.TILE).toInt()
        val ty = floor(frontY / Tuning.TILE).toInt()
        if (Tiles.canPlaceWall(map, tx, ty, t.x, t.y, b.halfW)) {
            Tiles.placeWall(map, tx, ty)
            mats.blocks--
            flow.rebuild(map, floor(t.x / Tuning.TILE).toInt(), floor(t.y / Tuning.TILE).toInt(), FlowRebuildSystem.MAX_DIST)
        }
    }
}
