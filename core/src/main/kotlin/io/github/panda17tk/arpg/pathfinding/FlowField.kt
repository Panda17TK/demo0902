package io.github.panda17tk.arpg.pathfinding

import io.github.panda17tk.arpg.map.TileMap

/**
 * BFS distance field (in tiles) from a source tile over walkable tiles, 4-neighbour.
 * Ported from legacy systems/flowfield.js. Enemy AI consumes this in Phase 5.
 */
class FlowField(val width: Int, val height: Int) {
    private val dist = IntArray(width * height) { UNREACHABLE }

    fun distAt(tx: Int, ty: Int): Int =
        if (tx in 0 until width && ty in 0 until height) dist[ty * width + tx] else UNREACHABLE

    fun rebuild(map: TileMap, startTileX: Int, startTileY: Int, maxDist: Int = Int.MAX_VALUE) {
        dist.fill(UNREACHABLE)
        if (startTileX !in 0 until width || startTileY !in 0 until height) return
        val queue = ArrayDeque<Int>()
        dist[startTileY * width + startTileX] = 0
        queue.addLast(startTileY * width + startTileX)
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            val cx = cur % width
            val cy = cur / width
            val d = dist[cur]
            for (n in NEIGHBOURS) {
                val nx = cx + n[0]
                val ny = cy + n[1]
                if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue
                if (map.solidAt(nx, ny)) continue
                val ni = ny * width + nx
                if (d + 1 <= maxDist && dist[ni] > d + 1) { dist[ni] = d + 1; queue.addLast(ni) }
            }
        }
    }

    companion object {
        const val UNREACHABLE = Int.MAX_VALUE
        private val NEIGHBOURS = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))
    }
}
