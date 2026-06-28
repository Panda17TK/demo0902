package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.floor

/** Result of advancing a bullet one step. wallTile != null means it hit a wall (caller damages it). */
data class BulletStep(val x: Float, val y: Float, val life: Float, val wallTile: Pair<Int, Int>?, val expired: Boolean)

object Ballistics {
    fun stepBullet(map: TileMap, x: Float, y: Float, vx: Float, vy: Float, life: Float, dt: Float): BulletStep {
        val nx = x + vx * dt
        val ny = y + vy * dt
        val nLife = life - dt
        val tx = floor(nx / Tuning.TILE).toInt()
        val ty = floor(ny / Tuning.TILE).toInt()
        if (map.solidAt(tx, ty)) return BulletStep(nx, ny, nLife, tx to ty, true)
        return BulletStep(nx, ny, nLife, null, nLife <= 0f)
    }
}
