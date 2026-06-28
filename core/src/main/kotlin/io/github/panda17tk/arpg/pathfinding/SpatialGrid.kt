package io.github.panda17tk.arpg.pathfinding

import kotlin.math.floor

/**
 * Uniform-grid broad-phase for neighbour queries (ported from legacy systems/spatial.js).
 * Generic over item type; rebuilt each frame by combat/AI once enemies exist (Phase 5).
 */
class SpatialGrid<T>(private val cell: Float) {
    private val buckets = HashMap<Long, MutableList<T>>()

    private fun key(cx: Int, cy: Int): Long = (cx.toLong() + ORIGIN) * STRIDE + (cy.toLong() + ORIGIN)

    fun clear() = buckets.clear()

    fun insert(item: T, x: Float, y: Float) {
        val k = key(floor(x / cell).toInt(), floor(y / cell).toInt())
        buckets.getOrPut(k) { mutableListOf() }.add(item)
    }

    fun forNearby(x: Float, y: Float, radius: Float, action: (T) -> Unit) {
        val mincx = floor((x - radius) / cell).toInt()
        val maxcx = floor((x + radius) / cell).toInt()
        val mincy = floor((y - radius) / cell).toInt()
        val maxcy = floor((y + radius) / cell).toInt()
        for (cy in mincy..maxcy) for (cx in mincx..maxcx) {
            buckets[key(cx, cy)]?.forEach(action)
        }
    }

    companion object {
        private const val ORIGIN = 1L shl 14
        private const val STRIDE = 1L shl 15
    }
}
