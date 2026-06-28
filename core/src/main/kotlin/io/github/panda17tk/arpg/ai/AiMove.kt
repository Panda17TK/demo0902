package io.github.panda17tk.arpg.ai

import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.pathfinding.FlowField
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.floor
import kotlin.math.sqrt

object AiMove {
    private val DIRS = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))

    /** Unit direction toward the player along the flow-field, or (0,0) if no downhill walkable neighbour. */
    fun followDir(map: TileMap, flow: FlowField, mobX: Float, mobY: Float): Pair<Float, Float> {
        val tx = floor(mobX / Tuning.TILE).toInt()
        val ty = floor(mobY / Tuning.TILE).toInt()
        val here = flow.distAt(tx, ty)
        var best = here
        var bx = 0; var by = 0
        for (d in DIRS) {
            val nx = tx + d[0]; val ny = ty + d[1]
            if (map.solidAt(nx, ny)) continue
            val v = flow.distAt(nx, ny)
            if (v < best) { best = v; bx = d[0]; by = d[1] }
        }
        if (bx == 0 && by == 0) return 0f to 0f
        val len = sqrt((bx * bx + by * by).toFloat())
        return (bx / len) to (by / len)
    }
}
