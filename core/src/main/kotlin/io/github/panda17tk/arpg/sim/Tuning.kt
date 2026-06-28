package io.github.panda17tk.arpg.sim

/** Gameplay tuning ported from legacy CONFIG.player / combat.js. */
object Tuning {
    const val BASE_SPEED = 110f
    const val SPEED_MUL = 1.2f
    const val DASH_MUL = 2f
    const val HP_MAX = 100f
    const val STA_MAX = 100f
    const val STA_DRAIN = 35f   // stamina/sec while dashing
    const val STA_REGEN = 22f   // stamina/sec while not dashing
    const val PLAYER_RADIUS = 11f
    const val PLAYER_HALF = 11f       // AABB half-extent (legacy w=h=22)
    const val PLACED_WALL_HP = 60f    // HP of a player-placed wall
    const val START_MATERIALS = 2     // legacy player.inv.blocks initial

    const val TILE = 32f
    const val VIEW_W = 800f
    const val VIEW_H = 480f
    const val CAM_LOOK_AHEAD = 36f
}
