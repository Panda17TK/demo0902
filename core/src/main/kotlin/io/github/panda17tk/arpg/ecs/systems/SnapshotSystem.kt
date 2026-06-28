package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.Transform

class SnapshotSystem : IteratingSystem(family { all(Transform) }) {
    override fun onTickEntity(entity: Entity) {
        val t = entity[Transform]
        t.prevX = t.x
        t.prevY = t.y
    }
}
