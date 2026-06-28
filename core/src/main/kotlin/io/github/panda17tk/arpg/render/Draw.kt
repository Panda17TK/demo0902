package io.github.panda17tk.arpg.render

import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/** Shape primitives for the procedural look. Call inside a `shapes.begin(Filled)` pass. */
object Draw {
    /** Filled rounded rect (Canvas roundedRect): center cross + 4 corner circles. (x,y) = bottom-left. */
    fun roundedRect(s: ShapeRenderer, x: Float, y: Float, w: Float, h: Float, r0: Float) {
        val r = minOf(r0, w / 2f, h / 2f)
        if (r <= 0f) { s.rect(x, y, w, h); return }
        s.rect(x + r, y, w - 2f * r, h)
        s.rect(x, y + r, w, h - 2f * r)
        s.circle(x + r, y + r, r, 10)
        s.circle(x + w - r, y + r, r, 10)
        s.circle(x + r, y + h - r, r, 10)
        s.circle(x + w - r, y + h - r, r, 10)
    }

    /** Filled rect oriented along unit (dirX,dirY): spans [fromD, fromD+len] from (cx,cy), half-width hw. */
    fun orientedRect(s: ShapeRenderer, cx: Float, cy: Float, dirX: Float, dirY: Float, fromD: Float, len: Float, hw: Float) {
        val px = -dirY; val py = dirX // perpendicular
        val n0x = cx + dirX * fromD; val n0y = cy + dirY * fromD
        val n1x = cx + dirX * (fromD + len); val n1y = cy + dirY * (fromD + len)
        val ax = n0x + px * hw; val ay = n0y + py * hw
        val bx = n0x - px * hw; val by = n0y - py * hw
        val cX = n1x - px * hw; val cY = n1y - py * hw
        val dX = n1x + px * hw; val dY = n1y + py * hw
        s.triangle(ax, ay, bx, by, cX, cY)
        s.triangle(ax, ay, cX, cY, dX, dY)
    }
}
