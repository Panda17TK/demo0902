package io.github.panda17tk.arpg.sim

import java.util.BitSet

/**
 * Fog-of-war exploration record (v2.33 インベントリのマップ): one bit per tile, set as the player
 * walks. The inventory MAP tab draws only tiles the player has actually visited. Pure data —
 * no libGDX — so marking/reading is unit-testable.
 */
class VisitedMap(val w: Int, val h: Int) {
    private val bits = BitSet(w * h)

    fun mark(tx: Int, ty: Int, radius: Int = 0) {
        for (y in ty - radius..ty + radius) {
            for (x in tx - radius..tx + radius) {
                if (x in 0 until w && y in 0 until h) bits.set(y * w + x)
            }
        }
    }

    fun visited(tx: Int, ty: Int): Boolean = tx in 0 until w && ty in 0 until h && bits.get(ty * w + tx)

    val count: Int get() = bits.cardinality()

    /** Visit every marked tile (row-major) — the MAP tab's draw loop. */
    fun forEachVisited(action: (tx: Int, ty: Int) -> Unit) {
        var i = bits.nextSetBit(0)
        while (i >= 0) {
            action(i % w, i / w)
            i = bits.nextSetBit(i + 1)
        }
    }
}
