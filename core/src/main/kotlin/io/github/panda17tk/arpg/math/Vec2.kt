package io.github.panda17tk.arpg.math

import kotlin.math.sqrt

/** Immutable 2D float vector. World convention is y-down. */
data class Vec2(val x: Float, val y: Float) {
    fun length(): Float = sqrt(x * x + y * y)
    fun normalized(): Vec2 {
        val len = length()
        return if (len == 0f) ZERO else Vec2(x / len, y / len)
    }
    operator fun plus(o: Vec2): Vec2 = Vec2(x + o.x, y + o.y)
    operator fun times(s: Float): Vec2 = Vec2(x * s, y * s)

    companion object { val ZERO = Vec2(0f, 0f) }
}
