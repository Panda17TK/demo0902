package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.floor

/** [wallTileX]/[wallTileY] are the solid tile the beam stopped on (-1 if it reached max range hitting nothing). */
data class BeamHit(val endX: Float, val endY: Float, val reach: Float, val wallTileX: Int = -1, val wallTileY: Int = -1) {
    val hitWall: Boolean get() = wallTileX >= 0
}

/** Ray-march to the first solid tile (legacy beam). step=6, walks until a wall or maxLen. */
object BeamRay {
    private const val STEP = 6f
    fun cast(map: TileMap, x: Float, y: Float, dirX: Float, dirY: Float, maxLen: Float): BeamHit {
        var cx = x; var cy = y; var ex = x; var ey = y; var reach = maxLen
        var t = 0f
        while (t < maxLen) {
            cx += dirX * STEP; cy += dirY * STEP
            val tx = floor(cx / Tuning.TILE).toInt(); val ty = floor(cy / Tuning.TILE).toInt()
            if (map.solidAt(tx, ty)) { return BeamHit(ex, ey, t, tx, ty) } // stop at the wall, report which tile
            ex = cx; ey = cy
            t += STEP
        }
        return BeamHit(ex, ey, reach)
    }
}
