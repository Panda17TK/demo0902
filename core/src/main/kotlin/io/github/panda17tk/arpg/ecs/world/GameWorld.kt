package io.github.panda17tk.arpg.ecs.world

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

/** Bundles the ECS world with the player entity for convenient rendering access. */
class GameWorld(val world: World, val player: Entity)
