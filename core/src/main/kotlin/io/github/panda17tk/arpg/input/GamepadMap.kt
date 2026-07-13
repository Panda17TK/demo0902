package io.github.panda17tk.arpg.input

import kotlin.math.hypot

/**
 * v2.188 ゲームパッド: pure stick/trigger mapping — NO gdx-controllers import, so it unit-tests like
 * TouchLayout. The thin [GamepadInput] source reads the physical device and calls these, feeding
 * InputState exactly the way the on-screen twin-stick does. Deadzone → renormalized unit vector +
 * 0..1 magnitude, so the stick starts at 0 just outside the dead ring (no snap at the edge).
 */
object GamepadMap {
    const val MOVE_DEAD = 0.25f
    const val AIM_DEAD = 0.30f
    const val TRIGGER_ON = 0.5f // an analog trigger/axis counts as pressed past this

    /** A stick reading: [active] is false inside the deadzone; [x],[y] form a unit vector; [mag] is 0..1. */
    data class Stick(val active: Boolean, val x: Float, val y: Float, val mag: Float)

    fun stick(rawX: Float, rawY: Float, deadzone: Float): Stick {
        val len = hypot(rawX, rawY)
        if (len <= deadzone) return Stick(false, 0f, 0f, 0f)
        val mag = ((len - deadzone) / (1f - deadzone)).coerceIn(0f, 1f)
        return Stick(true, rawX / len, rawY / len, mag)
    }

    fun pressed(axis: Float): Boolean = axis >= TRIGGER_ON
}
