package io.github.panda17tk.arpg.ui

import kotlin.math.abs

/**
 * v2.108 ナビ — pure math for the screen-edge direction markers: given a POI's offset from the
 * camera (WORLD units, y-down) and both viewport sizes, where does its marker sit on the HUD
 * (y-up)? Returns null while the POI is on screen — a visible thing needs no pointer.
 */
object EdgeMarkers {
    fun place(
        dxWorld: Float, dyWorld: Float,
        viewW: Float, viewH: Float,
        hudW: Float, hudH: Float,
        margin: Float,
    ): Pair<Float, Float>? {
        if (viewW <= 0f || viewH <= 0f) return null
        val fx = dxWorld / (viewW / 2f)
        val fy = -dyWorld / (viewH / 2f) // world y-down → HUD y-up
        if (abs(fx) <= 1f && abs(fy) <= 1f) return null // visible
        val k = maxOf(abs(fx), abs(fy))
        return (hudW / 2f + fx / k * (hudW / 2f - margin)) to (hudH / 2f + fy / k * (hudH / 2f - margin))
    }
}
