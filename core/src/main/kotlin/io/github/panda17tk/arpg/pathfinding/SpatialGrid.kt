package io.github.panda17tk.arpg.pathfinding

import kotlin.math.floor

/**
 * Uniform-grid broad-phase for neighbour queries (ported from legacy systems/spatial.js).
 * Generic over item type; rebuilt each tick by combat/AI once enemies exist (Phase 5).
 *
 * v2.176 GCの静音化: the old HashMap<Long, MutableList<T>> boxed EVERY key — ~5000 inserts plus
 * thousands of lookups per tick made tens of thousands of Long boxes per second on Android.
 * Open addressing over primitive arrays keeps the exact same behaviour (cell walk order and
 * per-bucket insertion order are untouched, so the sim stays byte-identical) with zero boxing.
 * Slots are only ever freed wholesale in [clear], so probe chains never break.
 */
class SpatialGrid<T>(private val cell: Float) {
    private var capacity = 1 shl 11 // power of two
    private var mask = capacity - 1
    private var keys = LongArray(capacity) { EMPTY }
    private var lists = arrayOfNulls<MutableList<T>>(capacity)
    private var usedSlots = IntArray(256)
    private var usedCount = 0
    // v2.140 シムの節約: recycle the bucket lists instead of allocating fresh ones every tick.
    private val freeLists = ArrayDeque<MutableList<T>>()

    // Bijective key for 32-bit cell coords (collision-free including negative coordinates).
    private fun key(cx: Int, cy: Int): Long = (cx.toLong() shl 32) or (cy.toLong() and 0xFFFFFFFFL)

    /** The slot holding [k], or the first empty slot of its probe chain. */
    private fun slotOf(k: Long): Int {
        var i = ((k xor (k ushr 32)).toInt() * -0x61c88647) and mask
        while (true) {
            val cur = keys[i]
            if (cur == k || cur == EMPTY) return i
            i = (i + 1) and mask
        }
    }

    fun clear() {
        for (s in 0 until usedCount) {
            val i = usedSlots[s]
            lists[i]?.let { it.clear(); freeLists.addLast(it) }
            lists[i] = null
            keys[i] = EMPTY
        }
        usedCount = 0
    }

    fun insert(item: T, x: Float, y: Float) {
        if ((usedCount + 1) * 4 >= capacity * 3) grow() // load factor stays under 3/4
        val k = key(floor(x / cell).toInt(), floor(y / cell).toInt())
        val i = slotOf(k)
        if (keys[i] != k) {
            keys[i] = k
            lists[i] = freeLists.removeLastOrNull() ?: mutableListOf()
            if (usedCount == usedSlots.size) usedSlots = usedSlots.copyOf(usedSlots.size * 2)
            usedSlots[usedCount++] = i
        }
        lists[i]!!.add(item)
    }

    fun forNearby(x: Float, y: Float, radius: Float, action: (T) -> Unit) {
        val mincx = floor((x - radius) / cell).toInt()
        val maxcx = floor((x + radius) / cell).toInt()
        val mincy = floor((y - radius) / cell).toInt()
        val maxcy = floor((y + radius) / cell).toInt()
        for (cy in mincy..maxcy) for (cx in mincx..maxcx) {
            val i = slotOf(key(cx, cy))
            if (keys[i] != EMPTY) lists[i]?.forEach(action)
        }
    }

    private fun grow() {
        val oldKeys = keys
        val oldLists = lists
        val oldUsed = usedSlots
        val oldCount = usedCount
        capacity = capacity shl 1
        mask = capacity - 1
        keys = LongArray(capacity) { EMPTY }
        lists = arrayOfNulls(capacity)
        usedCount = 0
        for (s in 0 until oldCount) {
            val oi = oldUsed[s]
            val i = slotOf(oldKeys[oi])
            keys[i] = oldKeys[oi]
            lists[i] = oldLists[oi]
            usedSlots[usedCount++] = i
        }
    }

    private companion object {
        // cx = Int.MIN_VALUE would collide — that is a cell ~2^31 tiles out; no map comes close.
        const val EMPTY = Long.MIN_VALUE
    }
}
