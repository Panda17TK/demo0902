package io.github.panda17tk.arpg.sim

/**
 * v2.164 軽い海 — distance LOD for the 30× ocean (v2.144). The sky holds ~5,000 wildlife now;
 * a mid-tier phone cannot run boids, senses and predation for all of them every frame. Wildlife
 * far from the keeper ticks on a stagger instead — every 4th frame past ~1.5 screens, every 8th
 * past ~3 — with the SAME total dt (the skipped frames' dt is applied in one step), so a distant
 * school traces the same path at a fraction of the cost. Deterministic: the stagger keys on the
 * sim tick counter and the entity id, never on the wall clock.
 */
object WildLod {
    const val MID = 1500f // beyond ~1.5 screens the wild ticks at 1/4 rate
    const val FAR = 2800f // beyond ~3 screens, 1/8 (a fast fish's 8-frame step stays under a tile)
    const val MID2 = MID * MID
    const val FAR2 = FAR * FAR
    const val MID_STRIDE = 4
    const val FAR_STRIDE = 8

    /** v2.169 診断修正: within any weapon's reach the damage grid must stay COMPLETE — the
     *  railgun snipes to 1600px (FireSystem.RAIL_RANGE), farther than MID, so gating grid
     *  insertion at MID made rail slugs pass through fish in the 1500..1600 band on non-due
     *  ticks. Behaviour still strides at MID; only the hittable radius is wider. */
    const val GRID_KEEP = 1650f
    const val GRID_KEEP2 = GRID_KEEP * GRID_KEEP

    fun stride(d2: Float): Int = when {
        d2 > FAR2 -> FAR_STRIDE
        d2 > MID2 -> MID_STRIDE
        else -> 1
    }

    fun strideAt(x: Float, y: Float, px: Float, py: Float): Int {
        val dx = x - px; val dy = y - py
        return stride(dx * dx + dy * dy)
    }

    /** Whether this entity's turn falls on this tick — staggered by id so the load spreads flat. */
    fun due(stride: Int, tick: Int, id: Int): Boolean = stride == 1 || (tick + id) % stride == 0
}
