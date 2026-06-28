package io.github.panda17tk.arpg.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import io.github.panda17tk.arpg.map.Tile
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.sim.Tuning

/**
 * Space / asteroid-belt world rendering: a dark deterministic starfield floor and rocky asteroid
 * walls (rounded rock per tile + craters, mottled by a per-tile hash). Call inside a Filled pass
 * with GL blending enabled. World is y-down.
 */
object WorldView {
    private val SPACE_A = Color.valueOf("070a12")
    private val SPACE_B = Color.valueOf("0a0e18")
    private val STARS = arrayOf(Color.valueOf("ffffff"), Color.valueOf("9ec5ff"), Color.valueOf("ffe6b0"), Color.valueOf("cbb8ff"))
    private val ROCKS = arrayOf(Color.valueOf("6b6358"), Color.valueOf("5d564c"), Color.valueOf("776c5d"), Color.valueOf("534b42"))
    private val CRATER = Color.valueOf("38322b")
    private val DOOR = Color.valueOf("3b2a1a")
    private val DOOR_FRAME = Color.valueOf("6b4c2b")

    fun draw(s: ShapeRenderer, map: TileMap) {
        val t = Tuning.TILE
        val ti = t.toInt()
        for (ty in 0 until map.height) for (tx in 0 until map.width) {
            val px = tx * t; val py = ty * t
            val hsh = (tx * 73856093) xor (ty * 19349663)
            when (map.tileAt(tx, ty)) {
                Tile.WALL -> {
                    s.color = ROCKS[Math.floorMod(hsh, ROCKS.size)]
                    Draw.roundedRect(s, px + 1f, py + 1f, t - 2f, t - 2f, 7f)
                    s.color = CRATER
                    s.circle(px + 7f + Math.floorMod(hsh, 12).toFloat(), py + 7f + Math.floorMod(hsh ushr 3, 12).toFloat(), 2.6f, 7)
                    if (Math.floorMod(hsh, 3) == 0) {
                        s.circle(px + t - 9f - Math.floorMod(hsh ushr 5, 5).toFloat(), py + t - 9f - Math.floorMod(hsh ushr 7, 5).toFloat(), 1.7f, 6)
                    }
                }
                Tile.DOOR -> {
                    s.color = DOOR; s.rect(px, py, t, t)
                    s.color = DOOR_FRAME
                    s.rect(px + 6f, py + 4f, t - 12f, 1.5f); s.rect(px + 6f, py + t - 5.5f, t - 12f, 1.5f)
                    s.rect(px + 6f, py + 4f, 1.5f, t - 8f); s.rect(px + t - 7.5f, py + 4f, 1.5f, t - 8f)
                }
                else -> {
                    s.color = if (((tx + ty) and 1) == 0) SPACE_A else SPACE_B
                    s.rect(px, py, t, t)
                    if (Math.floorMod(hsh, 9) == 0) {
                        s.color = STARS[Math.floorMod(hsh ushr 4, STARS.size)]
                        val sz = if (Math.floorMod(hsh, 47) == 0) 2.4f else 1.3f
                        s.circle(px + Math.floorMod(hsh, ti).toFloat(), py + Math.floorMod(hsh ushr 3, ti).toFloat(), sz, 6)
                    }
                }
            }
        }
    }
}
