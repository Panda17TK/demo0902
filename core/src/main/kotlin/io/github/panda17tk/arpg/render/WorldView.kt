package io.github.panda17tk.arpg.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import io.github.panda17tk.arpg.map.Tile
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.sim.Tuning

/**
 * Procedural floor/wall rendering ported from legacy renderer.js tile loop. Call inside a Filled
 * pass with GL blending enabled (the floor edges/specks use alpha). World is y-down.
 */
object WorldView {
    private val FLOOR_A = Color.valueOf("141b25")
    private val FLOOR_B = Color.valueOf("121823")
    private val WALL_OUT = Color.valueOf("1b2735")
    private val WALL_IN = Color.valueOf("0f1620")
    private val DOOR = Color.valueOf("3b2a1a")
    private val DOOR_FRAME = Color.valueOf("6b4c2b")
    private val EDGE = Color(0f, 0f, 0f, 0.22f)
    private val SPECK = Color(1f, 1f, 1f, 0.05f)

    fun draw(s: ShapeRenderer, map: TileMap) {
        val t = Tuning.TILE
        val ti = t.toInt()
        for (ty in 0 until map.height) for (tx in 0 until map.width) {
            val px = tx * t; val py = ty * t
            when (map.tileAt(tx, ty)) {
                Tile.WALL -> {
                    s.color = WALL_OUT; s.rect(px, py, t, t)
                    s.color = WALL_IN; s.rect(px + 2f, py + 2f, t - 4f, t - 4f)
                }
                Tile.DOOR -> {
                    s.color = DOOR; s.rect(px, py, t, t)
                    s.color = DOOR_FRAME
                    s.rect(px + 6f, py + 4f, t - 12f, 1.5f)
                    s.rect(px + 6f, py + t - 5.5f, t - 12f, 1.5f)
                    s.rect(px + 6f, py + 4f, 1.5f, t - 8f)
                    s.rect(px + t - 7.5f, py + 4f, 1.5f, t - 8f)
                }
                else -> {
                    s.color = if (((tx + ty) and 1) == 0) FLOOR_A else FLOOR_B
                    s.rect(px, py, t, t)
                    s.color = EDGE
                    s.rect(px, py, t, 1f)
                    s.rect(px, py, 1f, t)
                    val hsh = (tx * 73856093) xor (ty * 19349663)
                    if (Math.floorMod(hsh, 7) == 0) {
                        s.color = SPECK
                        s.rect(px + Math.floorMod(hsh, ti).toFloat(), py + Math.floorMod(hsh shr 3, ti).toFloat(), 2f, 2f)
                    }
                }
            }
        }
    }
}
