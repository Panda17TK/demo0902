package io.github.panda17tk.arpg.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import io.github.panda17tk.arpg.map.Biome
import io.github.panda17tk.arpg.map.Biomes
import io.github.panda17tk.arpg.map.Tile
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.cos
import kotlin.math.sin

/**
 * Space / asteroid-belt world rendering: a dark deterministic starfield floor and rocky asteroid
 * walls (rounded rock per tile + craters, mottled by a per-tile hash). Call inside a Filled pass
 * with GL blending enabled. World is y-down.
 */
object WorldView {
    private val SPACE_A = Color.valueOf("070a12")
    private val SPACE_B = Color.valueOf("090c16")
    // Floor = deep space: smooth nebula regions (7×7-tile patches) instead of a per-tile grid.
    private val NEB_P = Color.valueOf("130a1f")
    private val NEB_B = Color.valueOf("0a1124")
    private val NEB_T = Color.valueOf("0c1a1a")
    private val NEBULA_FLOOR = arrayOf(SPACE_A, SPACE_A, SPACE_B, SPACE_A, SPACE_A, SPACE_A, NEB_P, NEB_B, NEB_T)
    private val STARS = arrayOf(Color.valueOf("ffffff"), Color.valueOf("9ec5ff"), Color.valueOf("ffe6b0"), Color.valueOf("cbb8ff"))
    private val ROCKS = arrayOf(Color.valueOf("6b6358"), Color.valueOf("5d564c"), Color.valueOf("776c5d"), Color.valueOf("534b42"))
    private val GRASS_ROCK = arrayOf(Color.valueOf("3e5a2e"), Color.valueOf("4a6b34"), Color.valueOf("355026"))
    private val SNOW_ROCK = arrayOf(Color.valueOf("c4d2e0"), Color.valueOf("aebfd2"), Color.valueOf("d6e2ee"))
    private val MAGMA_ROCK = arrayOf(Color.valueOf("8a2f1c"), Color.valueOf("b3441f"), Color.valueOf("d65a22"))
    private val CRATER = Color.valueOf("38322b")
    private val CRACK = Color.valueOf("17130d")
    private val DOOR = Color.valueOf("3b2a1a")
    private val DOOR_FRAME = Color.valueOf("6b4c2b")

    fun draw(s: ShapeRenderer, map: TileMap, minTx: Int, maxTx: Int, minTy: Int, maxTy: Int) {
        val t = Tuning.TILE
        val ti = t.toInt()
        for (ty in minTy..maxTy) for (tx in minTx..maxTx) {
            val px = tx * t; val py = ty * t
            val hsh = (tx * 73856093) xor (ty * 19349663)
            when (map.tileAt(tx, ty)) {
                Tile.WALL -> {
                    s.color = when (Biomes.of(tx, ty)) {
                        Biome.GRASS -> GRASS_ROCK[Math.floorMod(hsh, GRASS_ROCK.size)]
                        Biome.SNOW -> SNOW_ROCK[Math.floorMod(hsh, SNOW_ROCK.size)]
                        Biome.MAGMA -> MAGMA_ROCK[Math.floorMod(hsh, MAGMA_ROCK.size)]
                        else -> ROCKS[Math.floorMod(hsh, ROCKS.size)]
                    }
                    Draw.roundedRect(s, px + 1f, py + 1f, t - 2f, t - 2f, 7f)
                    s.color = CRATER
                    s.circle(px + 7f + Math.floorMod(hsh, 12).toFloat(), py + 7f + Math.floorMod(hsh ushr 3, 12).toFloat(), 2.6f, 7)
                    if (Math.floorMod(hsh, 3) == 0) {
                        s.circle(px + t - 9f - Math.floorMod(hsh ushr 5, 5).toFloat(), py + t - 9f - Math.floorMod(hsh ushr 7, 5).toFloat(), 1.7f, 6)
                    }
                    // damage cracks: more/wider as HP drops (destructible asteroids only)
                    val mi = map.index(tx, ty); val mh = map.maxHp[mi]
                    if (mh.isFinite()) {
                        val dmg = (1f - map.hp[mi] / mh).coerceIn(0f, 1f)
                        if (dmg > 0.05f) cracks(s, px, py, t, hsh, dmg)
                    }
                }
                Tile.DOOR -> {
                    s.color = DOOR; s.rect(px, py, t, t)
                    s.color = DOOR_FRAME
                    s.rect(px + 6f, py + 4f, t - 12f, 1.5f); s.rect(px + 6f, py + t - 5.5f, t - 12f, 1.5f)
                    s.rect(px + 6f, py + 4f, 1.5f, t - 8f); s.rect(px + t - 7.5f, py + 4f, 1.5f, t - 8f)
                }
                else -> {
                    // deep-space floor: large nebula regions (no tile grid) + a sparse starfield
                    val reg = Math.floorMod((tx / 7) * 92821 xor (ty / 7) * 68917, NEBULA_FLOOR.size)
                    s.color = NEBULA_FLOOR[reg]
                    s.rect(px, py, t, t)
                    if (Math.floorMod(hsh, 6) == 0) {
                        s.color = STARS[Math.floorMod(hsh ushr 4, STARS.size)]
                        val sz = if (Math.floorMod(hsh, 41) == 0) 2.5f else 1.2f
                        s.circle(px + Math.floorMod(hsh, ti).toFloat(), py + Math.floorMod(hsh ushr 3, ti).toFloat(), sz, 6)
                    }
                }
            }
        }
    }

    /** Fracture lines radiating from a damaged asteroid tile (count + width scale with damage). */
    private fun cracks(s: ShapeRenderer, px: Float, py: Float, t: Float, hsh: Int, dmg: Float) {
        s.color = CRACK
        val cx = px + t / 2f; val cy = py + t / 2f
        val n = 1 + (dmg * 3f).toInt()
        for (k in 0 until n) {
            val ang = Math.floorMod(hsh + k * 53, 360).toFloat() * (Math.PI.toFloat() / 180f)
            val len = t * 0.30f + dmg * (t * 0.28f)
            Draw.orientedRect(s, cx, cy, cos(ang), sin(ang), -len * 0.35f, len, 0.9f + dmg)
        }
    }
}
