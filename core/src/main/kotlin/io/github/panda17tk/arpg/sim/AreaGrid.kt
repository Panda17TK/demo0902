package io.github.panda17tk.arpg.sim

/**
 * v2.166 宙域の九分割 — the sky is one shared plan, lived in nine slices. The full space stage
 * (≈2100×2100 tiles, ~4M-tile arrays) was the real weight on a phone: every takeoff allocated
 * tens of MB of tile/flow arrays and every frame walked a 5,000-strong ocean. An AREA world is
 * one 3×3 slice — 1/9 the tiles, 1/9 the flow field, 1/9 the fish — and reaching a slice's edge
 * crosses into the neighbouring one. Pure constants + geometry; WorldFactory slices the stage,
 * GameScreen drives the crossing.
 */
object AreaGrid {
    /** The sky divides 3×3; every run begins in the centre slice. */
    const val N = 3
    const val CENTER = 1

    /** Edge band (px, local coords) that triggers the crossing to a neighbour. */
    const val EDGE_TRIGGER = 80f

    /** Where the traveller re-emerges, measured in from the entered edge of the next slice —
     *  clear of both the wall ring and the trigger band, so arrival never re-fires the crossing. */
    const val ENTRY_INSET = 200f

    fun hasNeighbor(ax: Int, ay: Int, dx: Int, dy: Int): Boolean =
        (ax + dx) in 0 until N && (ay + dy) in 0 until N
}
