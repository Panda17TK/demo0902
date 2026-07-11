package io.github.panda17tk.arpg.sim

/** Engine/layout constants. Player/combat balance has moved to PlayerConfig. */
object Tuning {
    const val PLAYER_RADIUS = 11f
    const val PLAYER_HALF = 11f       // AABB half-extent (legacy w=h=22)
    const val PLACED_WALL_HP = 60f    // HP of a player-placed wall
    const val START_MATERIALS = 2     // legacy player.inv.blocks initial

    const val TILE = 32f
    // v2.147 巨体の手応え: broadphase query margin — must cover the widest creature half-extent
    // (isle_whale w=120 → 60). Queries sized for ~24px bodies left dead zones on whale-class hulls.
    const val MAX_BODY_HALF = 64f
    const val VIEW_W = 800f
    const val VIEW_H = 480f
    const val CAM_LOOK_AHEAD = 36f

    const val MUZZLE_OFFSET = 14f
    const val MELEE_WALL_OFFSET = 22f
    const val START_AMMO9 = 96
    const val START_AMMO12 = 24
    const val START_AMMO_BEAM = 6
    const val START_AMMO_NADE = 3

    // Surface event feed (LP v2.24)
    const val EVENT_FEED_MAX = 4      // most lines shown at once (oldest pushed out)
    const val EVENT_FEED_LIFE = 6f    // seconds a line lives
    const val EVENT_FEED_FADE = 1.5f  // it fades over its last this-many seconds
}
