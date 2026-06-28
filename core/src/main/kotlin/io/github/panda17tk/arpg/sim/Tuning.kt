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

    // --- Combat (legacy CONFIG.player / weapons) ---
    const val BULLET_SPEED = 360f
    const val BULLET_LIFE = 0.9f
    const val MUZZLE_OFFSET = 14f
    const val GRENADE_SPEED = 280f
    const val GRENADE_FUSE = 1.0f
    const val AUTO_RELOAD_DELAY = 0.8f
    const val MELEE_DMG = 22f
    const val MELEE_REACH = 51f
    const val MELEE_CD = 0.32f
    const val MELEE_SLASH_DMG = 8f
    const val MELEE_STA_WEAK_BELOW = 0.40f
    const val MELEE_STA_SWORD_MIN = 0.20f
    const val MELEE_WEAK_MUL = 0.6f
    const val FIST_DMG = 8f
    const val MELEE_WALL_OFFSET = 22f
    const val EXPLODE_RADIUS = 70f
    const val EXPLODE_DMG = 110f
    const val EXPLODE_SELF_DMG = 25f
    const val EXPLODE_WALL_DMG = 120f
    const val START_AMMO9 = 96
    const val START_AMMO12 = 24
    const val START_AMMO_BEAM = 6
    const val START_AMMO_NADE = 3
}
