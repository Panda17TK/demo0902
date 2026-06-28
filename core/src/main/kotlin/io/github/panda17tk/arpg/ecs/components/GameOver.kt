package io.github.panda17tk.arpg.ecs.components

/** Mutable run flag injected into the world (set when the player dies). UI overlay is Phase 7. */
class GameOver(var isOver: Boolean = false, var kills: Int = 0)
